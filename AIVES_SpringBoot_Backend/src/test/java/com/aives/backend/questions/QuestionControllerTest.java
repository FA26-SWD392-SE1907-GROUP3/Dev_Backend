package com.aives.backend.questions;

import java.util.UUID;
import com.aives.backend.common.AccessControl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionControllerTest {
    private final NamedParameterJdbcTemplate db = mock(NamedParameterJdbcTemplate.class);
    private final AccessControl access = mock(AccessControl.class);
    private final QuestionController controller = new QuestionController(db, access);

    @Test
    void refusesInvalidPageSizeBeforeDatabaseQuery() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.list(UUID.randomUUID(), mock(Jwt.class), null, 101));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void refusesInvalidStatus() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> controller.list(UUID.randomUUID(), mock(Jwt.class), "UNSAFE", 50));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}
