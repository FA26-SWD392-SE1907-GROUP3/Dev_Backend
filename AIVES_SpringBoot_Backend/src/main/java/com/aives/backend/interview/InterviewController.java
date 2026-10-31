package com.aives.backend.interview;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.aives.backend.common.AccessControl;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class InterviewController {
    private final NamedParameterJdbcTemplate db;
    private final AccessControl access;

    public InterviewController(NamedParameterJdbcTemplate db, AccessControl access) {
        this.db = db; this.access = access;
    }

    public record AnswerRequest(@NotBlank @Size(max = 10000) String text) {}
    public record QuestionSnapshot(UUID id, UUID questionId, String text) {}
    public record CandidateData(UUID id, String status, String examStatus, String mode,
                                int questionCount, Instant start, Instant end, Instant examStart, Instant examEnd) {}

    @PostMapping("/exams/{examId}/attempts/start")
    @Transactional
    public Map<String, Object> start(@PathVariable UUID examId, @AuthenticationPrincipal Jwt jwt) {
        access.isStudent(jwt);
        MapSqlParameterSource p = new MapSqlParameterSource().addValue("exam", examId)
            .addValue("student", access.userId(jwt));
        // Row lock prevents two concurrent requests from creating duplicate attempts.
        List<CandidateData> rows = db.query("""
            SELECT ec.id, ec.status, ec.scheduled_start, ec.scheduled_end,
              e.status AS exam_status, e.selection_mode, e.main_question_count, e.starts_at, e.ends_at
            FROM exam_candidates ec JOIN exams e ON e.id=ec.exam_id
            WHERE ec.exam_id=:exam AND ec.student_id=:student FOR UPDATE OF ec
            """, p, (rs, i) -> new CandidateData(
            rs.getObject("id", UUID.class), rs.getString("status"), rs.getString("exam_status"),
            rs.getString("selection_mode"), rs.getInt("main_question_count"),
            rs.getTimestamp("scheduled_start").toInstant(), rs.getTimestamp("scheduled_end").toInstant(),
            rs.getTimestamp("starts_at") == null ? null : rs.getTimestamp("starts_at").toInstant(),
            rs.getTimestamp("ends_at") == null ? null : rs.getTimestamp("ends_at").toInstant()));
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "You are not a candidate for this exam");
        CandidateData c = rows.get(0);
        Instant now = Instant.now();
        if (!(c.status().equals("REGISTERED") || c.status().equals("READY")) ||
            !List.of("SCHEDULED", "ACTIVE").contains(c.examStatus()) ||
            now.isBefore(c.start()) || !now.isBefore(c.end()) ||
            c.examStart() == null || c.examEnd() == null ||
            now.isBefore(c.examStart()) || !now.isBefore(c.examEnd())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Exam not available during your scheduled slot");
        }
        if (!"RANDOM".equals(c.mode())) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                "ADAPTIVE assignment requires an AI policy; only RANDOM is implemented in this starter");
        }
        if (access.count("SELECT count(*) FROM exam_attempts WHERE candidate_id=:candidate",
            Map.of("candidate", c.id())) != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An attempt already exists");
        }
        List<QuestionSnapshot> selections = db.query("""
            SELECT q.id, q.content FROM exam_question_pool pool
              JOIN questions q ON q.id=pool.question_id
            WHERE pool.exam_id=:exam AND q.status='APPROVED'
            ORDER BY random() LIMIT :limit
            """, new MapSqlParameterSource().addValue("exam", examId).addValue("limit", c.questionCount()),
            (rs, i) -> new QuestionSnapshot(null, rs.getObject("id", UUID.class), rs.getString("content")));
        if (selections.size() != c.questionCount()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough approved questions in the pool");
        }
        UUID attemptId = UUID.randomUUID();
        db.update("""
            INSERT INTO exam_attempts(id,candidate_id,exam_id,status,started_at,last_activity_at)
            VALUES (:attempt,:candidate,:exam,'IN_PROGRESS',now(),now())
            """, new MapSqlParameterSource().addValue("attempt", attemptId)
            .addValue("candidate", c.id()).addValue("exam", examId));
        db.update("UPDATE exam_candidates SET status='IN_PROGRESS' WHERE id=:id", Map.of("id", c.id()));
        for (int i = 0; i < selections.size(); i++) {
            var question = selections.get(i);
            UUID assignedId = UUID.randomUUID();
            db.update("""
                INSERT INTO attempt_questions (id,attempt_id,exam_id,question_id,sequence_no,
                    question_text_snapshot,selection_mode,status,started_at)
                VALUES (:id,:attempt,:exam,:question,:sequence,:snapshot,'RANDOM',:status,:startedAt)
                """, new MapSqlParameterSource().addValue("id", assignedId)
                .addValue("attempt", attemptId).addValue("exam", examId)
                .addValue("question", question.questionId()).addValue("sequence", i + 1)
                .addValue("snapshot", question.text())
                .addValue("status", i == 0 ? "ASKING" : "PENDING")
                .addValue("startedAt", i == 0 ? java.sql.Timestamp.from(Instant.now()) : null));
            if (i == 0) openMainQuestion(assignedId, question.text());
        }
        return Map.of("attemptId", attemptId, "status", "IN_PROGRESS", "questionCount", selections.size());
    }

    @GetMapping("/attempts/{attemptId}/current")
    public Map<String, Object> current(@PathVariable UUID attemptId, @AuthenticationPrincipal Jwt jwt) {
        requireOwner(attemptId, jwt);
        var questions = db.queryForList("""
            SELECT aq.id, aq.sequence_no, aq.question_text_snapshot, aq.status
            FROM attempt_questions aq WHERE aq.attempt_id=:attempt AND aq.status='ASKING'
            ORDER BY aq.sequence_no LIMIT 1
            """, Map.of("attempt", attemptId));
        if (questions.isEmpty()) return Map.of("status", "NO_ACTIVE_QUESTION");
        return questions.get(0);
    }

    @GetMapping("/attempts/{attemptId}/turns")
    public List<Map<String, Object>> turns(@PathVariable UUID attemptId, @AuthenticationPrincipal Jwt jwt) {
        requireOwner(attemptId, jwt);
        return db.queryForList("""
            SELECT aq.sequence_no, it.id, it.turn_no, it.speaker, it.kind, it.text_content,
                   it.speech_status, it.created_at
            FROM attempt_questions aq JOIN interview_turns it ON it.attempt_question_id=aq.id
            WHERE aq.attempt_id=:attempt ORDER BY aq.sequence_no, it.turn_no
            """, Map.of("attempt", attemptId));
    }

    @PostMapping("/attempt-questions/{attemptQuestionId}/answers")
    @Transactional
    public Map<String, Object> answer(@PathVariable UUID attemptQuestionId, @AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody AnswerRequest req) {
        access.isStudent(jwt);
        var data = db.query("""
            SELECT aq.id, aq.attempt_id, aq.status, ea.status AS attempt_status
            FROM attempt_questions aq JOIN exam_attempts ea ON ea.id=aq.attempt_id
             JOIN exam_candidates ec ON ec.id=ea.candidate_id
            WHERE aq.id=:id AND ec.student_id=:student FOR UPDATE OF aq
            """, new MapSqlParameterSource().addValue("id", attemptQuestionId)
                .addValue("student", access.userId(jwt)),
            (rs, i) -> new AnswerContext(rs.getObject("attempt_id", UUID.class),
                rs.getString("status"), rs.getString("attempt_status")));
        if (data.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Active question not found");
        AnswerContext ctx = data.get(0);
        if (!"ASKING".equals(ctx.questionStatus()) || !"IN_PROGRESS".equals(ctx.attemptStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Question is not awaiting an answer");
        }
        UUID promptId = db.queryForObject("""
            SELECT id FROM interview_turns WHERE attempt_question_id=:question AND kind='MAIN_QUESTION'
            """, Map.of("question", attemptQuestionId), UUID.class);
        UUID answerId = UUID.randomUUID();
        db.update("""
            INSERT INTO interview_turns (id,attempt_question_id,turn_no,speaker,kind,reply_to_turn_id,text_content)
            VALUES (:id,:question,2,'STUDENT','MAIN_ANSWER',:prompt,:text)
            """, new MapSqlParameterSource().addValue("id", answerId)
            .addValue("question", attemptQuestionId).addValue("prompt", promptId)
            .addValue("text", req.text().trim()));
        db.update("UPDATE attempt_questions SET status='FINISHED',completed_at=now() WHERE id=:id",
            Map.of("id", attemptQuestionId));
        var next = db.query("""
            SELECT id,question_text_snapshot FROM attempt_questions
            WHERE attempt_id=:attempt AND status='PENDING' ORDER BY sequence_no LIMIT 1 FOR UPDATE
            """, Map.of("attempt", ctx.attemptId()),
            (rs, i) -> new QuestionSnapshot(rs.getObject("id", UUID.class), null,
                rs.getString("question_text_snapshot")));
        if (!next.isEmpty()) {
            QuestionSnapshot q = next.get(0);
            db.update("UPDATE attempt_questions SET status='ASKING',started_at=now() WHERE id=:id",
                Map.of("id", q.id()));
            openMainQuestion(q.id(), q.text());
            db.update("UPDATE exam_attempts SET last_activity_at=now() WHERE id=:id",
                Map.of("id", ctx.attemptId()));
            return Map.of("status", "NEXT_QUESTION_READY", "attemptId", ctx.attemptId());
        }
        db.update("UPDATE exam_attempts SET status='COMPLETED',ended_at=now(),last_activity_at=now() WHERE id=:id",
            Map.of("id", ctx.attemptId()));
        db.update("""
            UPDATE exam_candidates SET status='COMPLETED' WHERE id=(
                SELECT candidate_id FROM exam_attempts WHERE id=:attempt)
            """, Map.of("attempt", ctx.attemptId()));
        return Map.of("status", "COMPLETED", "attemptId", ctx.attemptId());
    }

    private void openMainQuestion(UUID assignedId, String text) {
        db.update("""
            INSERT INTO interview_turns(attempt_question_id,turn_no,speaker,kind,followup_number,text_content,started_at)
            VALUES (:assigned,1,'AI','MAIN_QUESTION',0,:text,now())
            """, new MapSqlParameterSource().addValue("assigned", assignedId).addValue("text", text));
    }

    private void requireOwner(UUID attemptId, Jwt jwt) {
        access.isStudent(jwt);
        int allowed = access.count("""
            SELECT count(*) FROM exam_attempts ea JOIN exam_candidates ec ON ec.id=ea.candidate_id
            WHERE ea.id=:attempt AND ec.student_id=:student
            """, Map.of("attempt", attemptId, "student", access.userId(jwt)));
        if (allowed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found");
    }

    private record AnswerContext(UUID attemptId, String questionStatus, String attemptStatus) {}
}
