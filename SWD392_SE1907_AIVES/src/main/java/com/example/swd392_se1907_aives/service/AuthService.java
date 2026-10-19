package com.example.swd392_se1907_aives.service;
import com.example.swd392_se1907_aives.domain.dto.request.AuthRequest;
import com.example.swd392_se1907_aives.domain.dto.response.AuthResponse;
import com.example.swd392_se1907_aives.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Locale;
@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    public AuthResponse authenticate(AuthRequest req) {
        if(req.getEmail() == null || req.getPassword() == null)
            throw new BadCredentialsException("Invalid credentials");
        var user=userRepository.findByEmailIgnoreCase(req.getEmail().trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if(!Boolean.TRUE.equals(user.getIsActive()) || !passwordEncoder.matches(req.getPassword(),user.getPasswordHash()))
            throw new BadCredentialsException("Invalid credentials");
        return AuthResponse.builder().authenticate(true)
            .token(jwtService.generateToken(user.getId().toString(), user.getEmail(), user.getRole())).build();
    }
}
