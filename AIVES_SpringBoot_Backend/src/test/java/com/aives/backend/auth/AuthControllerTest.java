package com.aives.backend.auth;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {
    @Test
    void rejectsNonexistentLogin() {
        NamedParameterJdbcTemplate db = mock(NamedParameterJdbcTemplate.class);
        when(db.query(anyString(), any(java.util.Map.class), any(RowMapper.class))).thenReturn(List.of());
        AuthController controller = new AuthController(db, mock(PasswordEncoder.class), mock(JwtEncoder.class), 120);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.login(new AuthController.LoginRequest("nobody@aives.local", "bad")));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}
