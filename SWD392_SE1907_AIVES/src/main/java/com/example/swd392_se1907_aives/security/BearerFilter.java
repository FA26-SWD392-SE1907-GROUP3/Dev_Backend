package com.example.swd392_se1907_aives.security;
import com.example.swd392_se1907_aives.repository.UserRepository;
import com.example.swd392_se1907_aives.domain.enums.UserStatus;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.io.IOException;
import java.util.List;
@Component
@RequiredArgsConstructor
public class BearerFilter extends OncePerRequestFilter {
 private final TokenStore tokens; private final UserRepository users;
 @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
  String header = request.getHeader("Authorization");
  if (header != null && header.startsWith("Bearer ")) {
   Integer id = tokens.userId(header.substring(7));
   if (id != null) users.findById(id).filter(u -> u.getUserStatus() == UserStatus.ACTIVE).ifPresent(u -> {
    var auth = new UsernamePasswordAuthenticationToken(u.getUserId(), null,
      List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().getRoleName().name())));
    SecurityContextHolder.getContext().setAuthentication(auth);
   });
  }
  chain.doFilter(request, response);
 }
}
