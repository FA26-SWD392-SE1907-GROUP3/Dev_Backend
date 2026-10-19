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
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/users")
public class UserController {
    private final UserService service;
    @GetMapping @PreAuthorize("hasRole('ADMIN')") public ApiResponse<List<UserResponse>> all(){return ApiResponse.<List<UserResponse>>builder().code(200).data(service.getAllUsers()).build();}
    @GetMapping("/{id}") @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.principal.id.toString()")
    public ApiResponse<UserResponse> one(@PathVariable UUID id){return ApiResponse.<UserResponse>builder().code(200).data(service.getUserById(id)).build();}
    @PostMapping @PreAuthorize("hasRole('ADMIN')") public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreationRequest req){return ApiResponse.<UserResponse>builder().code(201).data(service.createUser(req)).build();}
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.principal.id.toString()")
    public ApiResponse<UserResponse> edit(@PathVariable UUID id,@Valid @RequestBody UserUpdateRequest req){return ApiResponse.<UserResponse>builder().code(200).data(service.updateUser(id,req)).build();}
    @PutMapping("/{id}/password") @PreAuthorize("isAuthenticated()") public ApiResponse<UserResponse> password(@PathVariable UUID id,@Valid @RequestBody UserPasswordChangeRequest req){return ApiResponse.<UserResponse>builder().code(200).data(service.updateUserPassword(id,req)).build();}
    @PatchMapping("/{id}/deactivate") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<UserResponse> deactivate(@PathVariable UUID id){return ApiResponse.<UserResponse>builder().code(200).data(service.setActive(id,false)).build();}
    @PatchMapping("/{id}/activate") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<UserResponse> activate(@PathVariable UUID id){return ApiResponse.<UserResponse>builder().code(200).data(service.setActive(id,true)).build();}
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public void remove(@PathVariable("id") UUID id){service.setActive(id,false);}
}
