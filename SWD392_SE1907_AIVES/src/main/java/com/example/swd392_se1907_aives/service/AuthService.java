package com.example.swd392_se1907_aives.service;

import com.example.swd392_se1907_aives.domain.dto.request.AuthRequest;
import com.example.swd392_se1907_aives.domain.dto.response.AuthResponse;
import com.example.swd392_se1907_aives.exceptions.AppException;
import com.example.swd392_se1907_aives.exceptions.enums.ErrorCode;
import com.example.swd392_se1907_aives.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    JwtService jwtService;

    public AuthResponse authenticate(AuthRequest request) {
        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        String token = jwtService.generateToken(
                user.getUserId(),
                user.getEmail(),
                user.getUsername(),
                user.getCoverImage(),
                user.getRole().toString());

        return AuthResponse.builder()
                .authenticate(true)
                .token(token)
                .build();
    }
}

