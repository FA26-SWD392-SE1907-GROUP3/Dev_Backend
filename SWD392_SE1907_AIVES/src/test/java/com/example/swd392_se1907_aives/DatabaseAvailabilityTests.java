package com.example.swd392_se1907_aives;

import com.example.swd392_se1907_aives.bootstrap.web.GlobalExceptionHandler;
import com.example.swd392_se1907_aives.modules.identity.internal.middlewares.JwtAuthenticationFilter;
import com.example.swd392_se1907_aives.modules.identity.internal.repository.UserRepository;
import com.example.swd392_se1907_aives.modules.identity.internal.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DatabaseAvailabilityTests {
    static Stream<RuntimeException> databaseFailures() {
        return Stream.of(new CannotCreateTransactionException("pool exhausted"),
            new QueryTimeoutException("query timed out"),
            new TransactionTimedOutException("transaction timed out"),
            new DataAccessResourceFailureException("connection lost"));
    }

    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @ParameterizedTest @MethodSource("databaseFailures")
    void controllerDatabaseFailuresReturnRetryable503(RuntimeException failure) throws Exception {
        var mvc=MockMvcBuilders.standaloneSetup(new UnavailableController(failure))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/database-test")).andExpect(status().isServiceUnavailable())
            .andExpect(header().string("Retry-After","5"))
            .andExpect(jsonPath("$.code").value(503))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cơ sở dữ liệu")));
    }

    @ParameterizedTest @MethodSource("databaseFailures")
    void databaseFailureDuringAuthenticationDoesNotBecome401OrContinueTheRequest(RuntimeException failure) throws Exception {
        var jwt=mock(JwtService.class);
        var users=mock(UserRepository.class);
        var chain=mock(FilterChain.class);
        UUID id=UUID.randomUUID();
        when(jwt.validateToken("token")).thenReturn(true);
        when(jwt.extractUserId("token")).thenReturn(id.toString());
        when(users.findById(id)).thenThrow(failure);
        var request=new MockHttpServletRequest("GET","/api/v1/interviews/test");
        request.addHeader("Authorization","Bearer token");
        var response=new MockHttpServletResponse();
        new JwtAuthenticationFilter(jwt,users,new ObjectMapper()).doFilter(request,response,chain);
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Retry-After")).isEqualTo("5");
        assertThat(new ObjectMapper().readTree(response.getContentAsString()).path("code").asInt()).isEqualTo(503);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(chain);
    }

    @RestController static class UnavailableController {
        private final RuntimeException failure;
        UnavailableController(RuntimeException failure) { this.failure=failure; }
        @GetMapping("/database-test") public void unavailable() { throw failure; }
    }
}
