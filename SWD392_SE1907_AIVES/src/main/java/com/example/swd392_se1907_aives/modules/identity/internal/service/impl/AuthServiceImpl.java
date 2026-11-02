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
import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request.RegistrationRequest;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.response.RegistrationResponse;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.entity.User;
import java.time.OffsetDateTime;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override @Transactional
    public RegistrationResponse register(RegistrationRequest request) {
        String email=request.getEmail().trim().toLowerCase(Locale.ROOT);
        if(userRepository.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email đã được sử dụng.");
        var user=new User();
        user.setFullName(request.getFullName().trim());user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        // The existing role column is required. This placeholder grants no access:
        // pending accounts are inactive, get no token, and have no role in their public DTO.
        user.setRole("STUDENT");user.setIsActive(false);user.setRegistrationStatus("PENDING");
        var now=OffsetDateTime.now();user.setCreatedAt(now);user.setUpdatedAt(now);
        try { userRepository.saveAndFlush(user); }
        catch(DataIntegrityViolationException duplicate) { throw new ResponseStatusException(HttpStatus.CONFLICT,"Email đã được sử dụng."); }
        return new RegistrationResponse(email,"PENDING","Đăng ký thành công. Vui lòng chờ Admin phân quyền và kích hoạt tài khoản.");
    }

    @Override
    public AuthResponse authenticate(AuthRequest request) {
        if (request.getEmail() == null || request.getPassword() == null)
            throw new BadCredentialsException("Invalid credentials");
        var user =
                userRepository
                        .findByEmailIgnoreCase(request.getEmail().trim().toLowerCase(Locale.ROOT))
                        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            throw new BadCredentialsException("Invalid credentials");
        if(user.isPendingRegistration()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Tài khoản đang chờ Admin phân quyền và kích hoạt.");
        if(!Boolean.TRUE.equals(user.getIsActive())) throw new BadCredentialsException("Invalid credentials");
        return AuthResponse.builder()
                .authenticate(true)
                .token(
                        jwtService.generateToken(
                                user.getId().toString(), user.getEmail(), user.getRole()))
                .build();
    }
}
