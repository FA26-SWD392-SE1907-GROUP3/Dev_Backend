package com.example.swd392_se1907_aives.modules.identity.internal.service;

import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request.AuthRequest;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse authenticate(AuthRequest request);
}
