package com.aives.backend.courses;

import java.util.Map;
import java.util.UUID;
import com.aives.backend.common.AccessControl;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/students")
@PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
public class EnrollmentController {
    private final NamedParameterJdbcTemplate db;
    private final AccessControl access;
    public EnrollmentController(NamedParameterJdbcTemplate db, AccessControl access) {
        this.db = db; this.access = access;
    }

    @PostMapping("/{studentId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> enroll(@PathVariable UUID courseId, @PathVariable UUID studentId,
                                       @AuthenticationPrincipal Jwt jwt) {
        access.lecturer(courseId, jwt);
        int inserted = db.update("""
            INSERT INTO course_enrollments(course_id,student_id)
            SELECT :course, u.id FROM users u WHERE u.id=:student AND u.role='STUDENT' AND u.is_active=true
            ON CONFLICT (course_id,student_id) DO UPDATE SET enrollment_status='ACTIVE'
            """, Map.of("course", courseId, "student", studentId));
        if (inserted == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active student not found");
        return Map.of("courseId", courseId, "studentId", studentId, "enrollmentStatus", "ACTIVE");
    }
}
