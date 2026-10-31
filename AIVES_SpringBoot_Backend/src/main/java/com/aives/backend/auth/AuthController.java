package com.aives.backend.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final NamedParameterJdbcTemplate db;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final long ttl;

    public AuthController(NamedParameterJdbcTemplate db, PasswordEncoder passwords, JwtEncoder encoder,
                          @Value("${aives.security.token-minutes}") long ttl) {
        this.db = db; this.passwords = passwords; this.encoder = encoder; this.ttl = ttl;
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record UserSummary(UUID id, String name, String email, String role) {}
    public record LoginResponse(String tokenType, String accessToken, long expiresInSeconds, UserSummary user) {}

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        var found = db.query("SELECT id, full_name, email, role, password_hash FROM users " +
                "WHERE email=lower(:email) AND is_active=true",
            Map.of("email", req.email().trim()), (rs, row) -> new UserWithPassword(
                rs.getObject("id", UUID.class), rs.getString("full_name"),
                rs.getString("email"), rs.getString("role"), rs.getString("password_hash")));
        if (found.size() != 1 || !passwords.matches(req.password(), found.get(0).hash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        var user = found.get(0);
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("aives-backend")
            .issuedAt(now).expiresAt(now.plus(ttl, ChronoUnit.MINUTES))
            .subject(user.id().toString()).claim("role", user.role()).claim("name", user.name()).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
        return new LoginResponse("Bearer", token, ttl * 60,
            new UserSummary(user.id(), user.name(), user.email(), user.role()));
    }

    private record UserWithPassword(UUID id, String name, String email, String role, String hash) {}
}
