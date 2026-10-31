package com.example.swd392_se1907_aives.modules.identity.internal.service.impl;

import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request.AuthRequest;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.response.AuthResponse;
import com.example.swd392_se1907_aives.modules.identity.internal.repository.UserRepository;
import com.example.swd392_se1907_aives.modules.identity.internal.service.AuthService;
import com.example.swd392_se1907_aives.modules.identity.internal.service.JwtService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public AuthResponse authenticate(AuthRequest request) {
        if (request.getEmail() == null || request.getPassword() == null)
            throw new BadCredentialsException("Invalid credentials");
        var user =
                userRepository
                        .findByEmailIgnoreCase(request.getEmail().trim().toLowerCase(Locale.ROOT))
                        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!Boolean.TRUE.equals(user.getIsActive())
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            throw new BadCredentialsException("Invalid credentials");
        return AuthResponse.builder()
                .authenticate(true)
                .token(
                        jwtService.generateToken(
                                user.getId().toString(), user.getEmail(), user.getRole()))
                .build();
    }
}
