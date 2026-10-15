package com.aives.backend.common;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AccessControl {
    private final NamedParameterJdbcTemplate db;
    public AccessControl(NamedParameterJdbcTemplate db) { this.db = db; }

    public UUID userId(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    public String role(Jwt jwt) { return jwt.getClaimAsString("role"); }
    public boolean admin(Jwt jwt) { return "ADMIN".equals(role(jwt)); }

    public void courseExists(UUID courseId) {
        if (count("SELECT count(*) FROM courses WHERE id=:course", Map.of("course", courseId)) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
    }

    public void lecturer(UUID courseId, Jwt jwt) {
        courseExists(courseId);
        if (admin(jwt)) return;
        if (!"LECTURER".equals(role(jwt)) || count(
                "SELECT count(*) FROM course_lecturers WHERE course_id=:course AND lecturer_id=:uid",
                Map.of("course", courseId, "uid", userId(jwt))) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a lecturer of this course");
        }
    }

    public void member(UUID courseId, Jwt jwt) {
        courseExists(courseId);
        if (admin(jwt)) return;
        if ("LECTURER".equals(role(jwt))) { lecturer(courseId, jwt); return; }
        if (!"STUDENT".equals(role(jwt)) || count(
                "SELECT count(*) FROM course_enrollments WHERE course_id=:course AND student_id=:uid AND enrollment_status='ACTIVE'",
                Map.of("course", courseId, "uid", userId(jwt))) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not enrolled in this course");
        }
    }

    public void isStudent(Jwt jwt) {
        if (!"STUDENT".equals(role(jwt))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student role required");
        }
    }

    public int count(String sql, Map<String, ?> params) {
        return db.queryForObject(sql, params, Integer.class);
    }
}
