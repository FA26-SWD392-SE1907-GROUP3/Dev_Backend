package com.aives.backend.admin;

import java.util.Map;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.aives.backend.common.AccessControl;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final NamedParameterJdbcTemplate db;
    private final PasswordEncoder encoder;
    private final AccessControl access;

    public AdminController(NamedParameterJdbcTemplate db, PasswordEncoder encoder, AccessControl access) {
        this.db = db; this.encoder = encoder; this.access = access;
    }

    public enum Role { ADMIN, LECTURER, STUDENT }
    public record NewUser(@NotBlank String fullName, @Email @NotBlank String email,
                          @Size(min=8, max=72) @NotBlank String password, @NotNull Role role) {}
    public record NewCourse(@NotBlank String code, @NotBlank String name, String description) {}

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> user(@Valid @RequestBody NewUser req) {
        UUID id = UUID.randomUUID();
        db.update("""
            INSERT INTO users(id,full_name,email,password_hash,role)
            VALUES (:id,:name,:email,:hash,:role)
            """, new MapSqlParameterSource().addValue("id", id).addValue("name", req.fullName())
                .addValue("email", req.email().trim().toLowerCase(java.util.Locale.ROOT))
                .addValue("hash", encoder.encode(req.password())).addValue("role", req.role().name()));
        return Map.of("id", id, "role", req.role().name());
    }

    @PostMapping("/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> course(@Valid @RequestBody NewCourse req) {
        UUID id = UUID.randomUUID();
        db.update("""
            INSERT INTO courses(id,code,name,description) VALUES (:id,:code,:name,:description)
            """, new MapSqlParameterSource().addValue("id", id).addValue("code", req.code().trim())
                .addValue("name", req.name().trim()).addValue("description", req.description()));
        return Map.of("id", id);
    }

    @PostMapping("/courses/{courseId}/lecturers/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> assignLecturer(@PathVariable UUID courseId, @PathVariable UUID userId) {
        access.courseExists(courseId);
        int inserted = db.update("""
            INSERT INTO course_lecturers(course_id,lecturer_id)
            SELECT :course, u.id FROM users u WHERE u.id=:uid AND u.role='LECTURER' AND u.is_active=true
            """, Map.of("course", courseId, "uid", userId));
        if (inserted == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active lecturer not found");
        return Map.of("courseId", courseId, "lecturerId", userId);
    }
}
