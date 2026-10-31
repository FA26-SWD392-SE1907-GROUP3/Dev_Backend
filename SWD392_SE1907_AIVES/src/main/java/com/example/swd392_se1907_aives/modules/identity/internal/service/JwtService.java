package com.example.swd392_se1907_aives.modules.identity.internal.service;

public interface JwtService {
    String generateToken(String userId, String email, String role);

    String extractUserId(String token);

    boolean validateToken(String token);
}
