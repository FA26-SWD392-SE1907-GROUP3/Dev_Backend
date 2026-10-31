package com.aives.backend.rubrics;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.aives.backend.common.AccessControl;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/rubrics")
public class RubricController {
    private final NamedParameterJdbcTemplate db;
    private final AccessControl access;

    public RubricController(NamedParameterJdbcTemplate db, AccessControl access) {
        this.db = db; this.access = access;
    }

    public record Criterion(@NotBlank String name, String description,
                            @NotNull @DecimalMin("0.01") BigDecimal maxPoints) {}
    public record CreateRubric(@NotBlank String name, String description,
        @NotNull @DecimalMin("0.01") BigDecimal maxScore,
        @NotEmpty List<@Valid Criterion> criteria) {}

    @GetMapping
    public List<Map<String, Object>> list(@PathVariable UUID courseId, @AuthenticationPrincipal Jwt jwt) {
        access.member(courseId, jwt);
        String filter = "STUDENT".equals(access.role(jwt)) ? " AND status='PUBLISHED'" : "";
        return db.queryForList("SELECT id, course_id, name, description, max_score, status, created_at " +
                "FROM rubrics WHERE course_id=:course" + filter + " ORDER BY created_at DESC",
            Map.of("course", courseId));
    }

    @GetMapping("/{rubricId}/criteria")
    public List<Map<String, Object>> criteria(@PathVariable UUID courseId, @PathVariable UUID rubricId,
                                               @AuthenticationPrincipal Jwt jwt) {
        access.member(courseId, jwt);
        String filter = "STUDENT".equals(access.role(jwt)) ? " AND status='PUBLISHED'" : "";
        int exists = access.count("SELECT count(*) FROM rubrics WHERE id=:rubric AND course_id=:course" + filter,
            Map.of("rubric", rubricId, "course", courseId));
        if (exists == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rubric not found");
        return db.queryForList("SELECT id, criterion_name, description, max_points, sort_order FROM rubric_criteria " +
                "WHERE rubric_id=:rubric ORDER BY sort_order", Map.of("rubric", rubricId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    @Transactional
    public Map<String, Object> create(@PathVariable UUID courseId, @AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody CreateRubric req) {
        access.lecturer(courseId, jwt);
        BigDecimal total = req.criteria().stream().map(Criterion::maxPoints)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(req.maxScore()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sum of criteria must equal maxScore");
        }
        UUID id = UUID.randomUUID();
        db.update("INSERT INTO rubrics(id, course_id, created_by, name, description, max_score) " +
                "VALUES (:id,:course,:uid,:name,:description,:maxScore)",
            new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", id).addValue("course", courseId).addValue("uid", access.userId(jwt))
                .addValue("name", req.name()).addValue("description", req.description())
                .addValue("maxScore", req.maxScore()));
        for (int i = 0; i < req.criteria().size(); i++) {
            Criterion c = req.criteria().get(i);
            db.update("INSERT INTO rubric_criteria (rubric_id,criterion_name,description,max_points,sort_order) " +
                    "VALUES (:rubric,:name,:description,:maxPoints,:position)",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                    .addValue("rubric", id).addValue("name", c.name()).addValue("description", c.description())
                    .addValue("maxPoints", c.maxPoints()).addValue("position", i + 1));
        }
        return Map.of("id", id, "status", "DRAFT", "criteriaCount", req.criteria().size());
    }

    @PostMapping("/{rubricId}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    public Map<String, String> publish(@PathVariable UUID courseId, @PathVariable UUID rubricId,
                                        @AuthenticationPrincipal Jwt jwt) {
        access.lecturer(courseId, jwt);
        int updated = db.update("""
            UPDATE rubrics SET status='PUBLISHED'
            WHERE id=:rubric AND course_id=:course AND status='DRAFT'
            AND EXISTS (SELECT 1 FROM rubric_criteria WHERE rubric_id=:rubric)
            """, Map.of("rubric", rubricId, "course", courseId));
        if (updated == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Rubric must exist and be in DRAFT with criteria");
        return Map.of("status", "PUBLISHED");
    }
}
