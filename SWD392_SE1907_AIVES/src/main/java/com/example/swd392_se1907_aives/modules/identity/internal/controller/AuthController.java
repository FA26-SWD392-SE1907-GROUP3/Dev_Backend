package com.example.swd392_se1907_aives.modules.identity.internal.controller;

import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request.AuthRequest;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.response.AuthResponse;
import com.example.swd392_se1907_aives.modules.identity.internal.service.AuthService;
import com.example.swd392_se1907_aives.shared.web.ApiResponse;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ApiResponse.<AuthResponse>builder()
            .data(authService.authenticate(request))
            .build();
    }
}
