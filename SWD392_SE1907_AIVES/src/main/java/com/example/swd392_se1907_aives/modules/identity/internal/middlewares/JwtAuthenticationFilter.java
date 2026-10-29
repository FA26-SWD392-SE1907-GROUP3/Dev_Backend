package com.example.swd392_se1907_aives.modules.identity.internal.middlewares;

import com.example.swd392_se1907_aives.modules.identity.internal.repository.UserRepository;
import com.example.swd392_se1907_aives.modules.identity.internal.service.JwtService;
import com.example.swd392_se1907_aives.shared.web.DatabaseUnavailableResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
@Component @RequiredArgsConstructor @lombok.extern.slf4j.Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper json;
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
      throws ServletException, IOException {
        var header=req.getHeader("Authorization");
        if(header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication()==null){
            var token=header.substring(7);
            if(jwtService.validateToken(token)){
                try {
                    userRepository.findById(UUID.fromString(jwtService.extractUserId(token)))
                        .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                        .ifPresent(user -> SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(user, null,
                                List.of(new SimpleGrantedAuthority("ROLE_"+user.getRole())))));
                } catch(IllegalArgumentException ignored) {}
                catch(org.springframework.transaction.CannotCreateTransactionException
                    | org.springframework.transaction.TransactionTimedOutException
                    | org.springframework.dao.TransientDataAccessException
                    | org.springframework.dao.DataAccessResourceFailureException exception) {
                    log.warn("Database unavailable during authentication ({})", exception.getClass().getSimpleName());
                    resp.setStatus(503);
                    resp.setHeader("Retry-After", Integer.toString(DatabaseUnavailableResponse.RETRY_AFTER_SECONDS));
                    resp.setContentType("application/json");
                    resp.setCharacterEncoding("UTF-8");
                    json.writeValue(resp.getOutputStream(), DatabaseUnavailableResponse.body());
                    return;
                }
            }
        }
        chain.doFilter(req,resp);
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest req){
        var p=req.getServletPath();
        return p.startsWith("/api/v1/auth/") || p.equals("/actuator/health");
    }
}
