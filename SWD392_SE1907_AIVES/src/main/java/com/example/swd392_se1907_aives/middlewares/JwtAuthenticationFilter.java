package com.example.swd392_se1907_aives.middlewares;
import com.example.swd392_se1907_aives.repository.UserRepository;
import com.example.swd392_se1907_aives.service.JwtService;
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
@Component @RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;
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
            }
        }
        chain.doFilter(req,resp);
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest req){
        var p=req.getServletPath();
        return p.startsWith("/api/v1/auth/") || p.equals("/actuator/health");
    }
}
