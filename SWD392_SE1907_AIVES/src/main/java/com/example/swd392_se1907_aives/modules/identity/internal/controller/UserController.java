package com.example.swd392_se1907_aives.modules.identity.internal.controller;

import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request.UserCreationRequest;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request.UserPasswordChangeRequest;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request.UserUpdateRequest;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.response.UserResponse;
import com.example.swd392_se1907_aives.modules.identity.internal.service.UserService;
import com.example.swd392_se1907_aives.shared.web.ApiResponse;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> all() {
        return ApiResponse.<List<UserResponse>>builder()
                .code(200)
                .data(service.getAllUsers())
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.principal.id.toString()")
    public ApiResponse<UserResponse> one(@PathVariable UUID id) {
        return ApiResponse.<UserResponse>builder().code(200).data(service.getUserById(id)).build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .code(201)
                .data(service.createUser(request))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.principal.id.toString()")
    public ApiResponse<UserResponse> edit(
            @PathVariable UUID id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .code(200)
                .data(service.updateUser(id, request))
                .build();
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserResponse> password(
            @PathVariable UUID id, @Valid @RequestBody UserPasswordChangeRequest request) {
        return ApiResponse.<UserResponse>builder()
                .code(200)
                .data(service.updateUserPassword(id, request))
                .build();
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> deactivate(@PathVariable UUID id) {
        return ApiResponse.<UserResponse>builder()
                .code(200)
                .data(service.setActive(id, false))
                .build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> activate(@PathVariable UUID id) {
        return ApiResponse.<UserResponse>builder()
                .code(200)
                .data(service.setActive(id, true))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void remove(@PathVariable("id") UUID id) {
        service.setActive(id, false);
    }
}
