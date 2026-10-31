package com.aives.backend.courses;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.aives.backend.common.AccessControl;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {
    private final NamedParameterJdbcTemplate db;
    private final AccessControl access;

    public CourseController(NamedParameterJdbcTemplate db, AccessControl access) {
        this.db = db; this.access = access;
    }

    @GetMapping
    public List<Map<String, Object>> courses(@AuthenticationPrincipal Jwt jwt) {
        return db.queryForList("""
            SELECT c.id, c.code, c.name, c.description, c.is_active
            FROM courses c WHERE c.is_active = true AND (
                :role = 'ADMIN' OR
                EXISTS (SELECT 1 FROM course_lecturers cl WHERE cl.course_id=c.id AND cl.lecturer_id=:uid) OR
                EXISTS (SELECT 1 FROM course_enrollments ce WHERE ce.course_id=c.id AND ce.student_id=:uid AND ce.enrollment_status='ACTIVE')
            ) ORDER BY c.code
            """, Map.of("role", access.role(jwt), "uid", access.userId(jwt)));
    }

    @GetMapping("/{courseId}/topics")
    public List<Map<String, Object>> topics(@PathVariable UUID courseId, @AuthenticationPrincipal Jwt jwt) {
        access.member(courseId, jwt);
        return db.queryForList("SELECT id, course_id, name, slug, description FROM course_topics " +
            "WHERE course_id=:course ORDER BY name", Map.of("course", courseId));
    }
}
