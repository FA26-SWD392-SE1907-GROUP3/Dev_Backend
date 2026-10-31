package com.aives.backend.questions;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.aives.backend.common.AccessControl;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/questions")
@PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
public class QuestionController {
    private final NamedParameterJdbcTemplate db;
    private final AccessControl access;
    public QuestionController(NamedParameterJdbcTemplate db, AccessControl access) {
        this.db = db; this.access = access;
    }

    public enum Bloom { REMEMBER, UNDERSTAND, APPLY, ANALYZE }
    public enum Difficulty { EASY, MEDIUM, HARD }
    public enum Decision { APPROVE, REJECT, REQUEST_CHANGES }
    public record WriteQuestion(@NotBlank String content, String referenceAnswer,
        @NotNull Bloom bloomLevel, @NotNull Difficulty difficulty, UUID topicId, UUID rubricId) {}
    public record ReviewQuestion(@NotNull Decision decision, String comment) {}

    @GetMapping
    public List<Map<String, Object>> list(@PathVariable UUID courseId, @AuthenticationPrincipal Jwt jwt,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(defaultValue = "50") int limit) {
        access.lecturer(courseId, jwt);
        if (limit < 1 || limit > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be 1..100");
        if (status != null && !List.of("DRAFT", "PENDING_REVIEW", "APPROVED", "REJECTED", "ARCHIVED").contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("course", courseId)
            .addValue("status", status).addValue("limit", limit);
        String query = """
            SELECT id, course_id, topic_id, rubric_id, content, reference_answer, bloom_level,
                   difficulty, source_type, status, created_by, created_at
            FROM questions WHERE course_id=:course
            """ + (status == null ? "" : " AND status=:status") + " ORDER BY created_at DESC LIMIT :limit";
        return db.queryForList(query, p);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@PathVariable UUID courseId, @AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody WriteQuestion req) {
        access.lecturer(courseId, jwt);
        UUID id = UUID.randomUUID();
        db.update("""
            INSERT INTO questions(id, course_id, topic_id, rubric_id, created_by,
              content, reference_answer, bloom_level, difficulty, source_type, status)
            VALUES (:id,:course,:topic,:rubric,:uid,:content,:answer,:bloom,:difficulty,'MANUAL','DRAFT')
            """, params(id, courseId, jwt, req));
        return Map.of("id", id, "status", "DRAFT");
    }

    @PutMapping("/{questionId}")
    public Map<String, Object> edit(@PathVariable UUID courseId, @PathVariable UUID questionId,
                                     @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody WriteQuestion req) {
        access.lecturer(courseId, jwt);
        int changed = db.update("""
            UPDATE questions SET topic_id=:topic, rubric_id=:rubric, content=:content,
                reference_answer=:answer, bloom_level=:bloom, difficulty=:difficulty, status='DRAFT',
                approved_at=NULL, approved_by=NULL
            WHERE id=:id AND course_id=:course AND source_type='MANUAL' AND status IN ('DRAFT','REJECTED')
            """, params(questionId, courseId, jwt, req));
        if (changed == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft/rejected manual questions can be edited");
        return Map.of("id", questionId, "status", "DRAFT");
    }

    private MapSqlParameterSource params(UUID id, UUID courseId, Jwt jwt, WriteQuestion req) {
        return new MapSqlParameterSource().addValue("id", id).addValue("course", courseId)
            .addValue("uid", access.userId(jwt)).addValue("topic", req.topicId())
            .addValue("rubric", req.rubricId()).addValue("content", req.content())
            .addValue("answer", req.referenceAnswer()).addValue("bloom", req.bloomLevel().name())
            .addValue("difficulty", req.difficulty().name());
    }

    @PostMapping("/{questionId}/submit")
    public Map<String, String> submit(@PathVariable UUID courseId, @PathVariable UUID questionId,
                                      @AuthenticationPrincipal Jwt jwt) {
        access.lecturer(courseId, jwt);
        int updated = db.update("""
            UPDATE questions SET status='PENDING_REVIEW' WHERE id=:id AND course_id=:course
            AND status='DRAFT'
            """, Map.of("id", questionId, "course", courseId));
        if (updated == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Question not in DRAFT");
        return Map.of("status", "PENDING_REVIEW");
    }

    @PostMapping("/{questionId}/review")
    @Transactional
    public Map<String, String> review(@PathVariable UUID courseId, @PathVariable UUID questionId,
                                       @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ReviewQuestion req) {
        access.lecturer(courseId, jwt);
        boolean approve = req.decision() == Decision.APPROVE;
        String status = approve ? "APPROVED" : req.decision() == Decision.REJECT ? "REJECTED" : "DRAFT";
        MapSqlParameterSource p = new MapSqlParameterSource()
            .addValue("course", courseId).addValue("id", questionId)
            .addValue("uid", access.userId(jwt)).addValue("status", status)
            .addValue("approve", approve);
        int changed = db.update("""
            UPDATE questions q SET status=:status,
              approved_by=CASE WHEN :approve THEN :uid ELSE NULL END,
              approved_at=CASE WHEN :approve THEN now() ELSE NULL END
            WHERE q.id=:id AND q.course_id=:course AND q.status='PENDING_REVIEW'
              AND (NOT :approve OR EXISTS (SELECT 1 FROM rubrics r
                   WHERE r.id=q.rubric_id AND r.course_id=q.course_id AND r.status='PUBLISHED'))
            """, p);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Question must be pending review; approval also requires a published rubric");
        db.update("""
            INSERT INTO question_reviews(question_id, reviewer_id, decision, comment)
            VALUES (:id,:uid,:decision,:comment)
            """, new MapSqlParameterSource().addValue("id", questionId)
            .addValue("uid", access.userId(jwt)).addValue("decision", req.decision().name())
            .addValue("comment", req.comment()));
        return Map.of("status", status);
    }
}
