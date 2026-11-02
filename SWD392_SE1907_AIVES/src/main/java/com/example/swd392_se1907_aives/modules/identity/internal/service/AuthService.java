package com.example.swd392_se1907_aives.modules.identity.internal.service;

import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request.AuthRequest;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.response.AuthResponse;

public interface AuthService {
    com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.response.RegistrationResponse register(
            com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request.RegistrationRequest request);
    AuthResponse authenticate(AuthRequest request);
}
