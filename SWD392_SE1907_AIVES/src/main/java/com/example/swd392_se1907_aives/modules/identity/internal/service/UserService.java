package com.example.swd392_se1907_aives.modules.identity.internal.service;

import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request.UserCreationRequest;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request.UserPasswordChangeRequest;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request.UserUpdateRequest;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {
    List<UserResponse> getAllUsers();

    UserResponse getUserById(UUID id);

    UserResponse createUser(UserCreationRequest request);

    UserResponse updateUser(UUID id, UserUpdateRequest request);

    UserResponse updateUserPassword(UUID id, UserPasswordChangeRequest request);

    UserResponse setActive(UUID id, boolean active);
}
