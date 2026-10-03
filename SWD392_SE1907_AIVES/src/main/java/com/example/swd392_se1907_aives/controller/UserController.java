package com.example.swd392_se1907_aives.controller;


import com.example.swd392_se1907_aives.domain.dto.request.UserCreationRequest;
import com.example.swd392_se1907_aives.domain.dto.request.UserPasswordChangeRequest;
import com.example.swd392_se1907_aives.domain.dto.request.UserUpdateRequest;
import com.example.swd392_se1907_aives.domain.dto.response.ApiResponse;
import com.example.swd392_se1907_aives.domain.dto.response.UserResponse;
import com.example.swd392_se1907_aives.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {
    UserService userService;

    /*
     * ================
     * Get All User
     * ===============
     */
    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsersApiResponse() {
        return ApiResponse.<List<UserResponse>>builder()
                .code(200)
                .message("Get all users successfully")
                .data(userService.getAllUsers())
                .build();
    }

    /*
     * ================
     * Get User by Id
     * ===============
     */
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable String id) {
        ApiResponse<UserResponse> response = new ApiResponse<>();

        response.setCode(200);
        response.setData(userService.getUserById(id));
        response.setMessage("User retrieved successfully");

        return response;
    }

    /*
     * ================
     * Create new user
     * ===============
     */
    @PostMapping
    public ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        ApiResponse<UserResponse> response = new ApiResponse<>();

        response.setCode(201);
        response.setData(userService.createUser(request));
        response.setMessage("User created successfully");

        return response;
    }

    /*
     * ================
     * Update user by Id
     * ===============
     */
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'ANNOTATOR', 'REVIEWER')")
    @Operation(
            summary = "Update user information",
            description = "Update user profile information (username, full name, email, etc.)"
    )
    public ApiResponse<UserResponse> updateUser(@PathVariable String id, @RequestBody @Valid UserUpdateRequest request) {
        ApiResponse<UserResponse> response = new ApiResponse<>();

        response.setCode(200);
        response.setData(userService.updateUser(id, request));
        response.setMessage("User updated successfully");

        return response;
    }

    /*
     * ================
     * Update user password
     * ===============
     */
    @PutMapping("/update/password/{id}")
    public ApiResponse<UserResponse> updateUserPassword(@PathVariable String id, @RequestBody @Valid UserPasswordChangeRequest request) {
        ApiResponse<UserResponse> response = new ApiResponse<>();
        response.setCode(200);
        response.setData(userService.updateUserPassword(id, request));
        response.setMessage("Password change successfully");

        return response;
    }

    /*
     * ================
     * Deactivate user
     * ===============
     */
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(
            summary = "Deactivate user",
            description = "Deactivate a user account (soft delete)"
    )
    public ApiResponse<UserResponse> deleteUser(@PathVariable String id) {
        ApiResponse<UserResponse> response = new ApiResponse<>();

        response.setCode(200);
        response.setMessage("Deactivate user successfully");
        response.setData(userService.deactivateUser(id));

        return response;
    }

    /*
     * ================
     * Activate user
     * ===============
     */
    @PatchMapping("/{id}/activate")
    public ApiResponse<UserResponse> activateUser(@PathVariable String id) {
        ApiResponse<UserResponse> response = new ApiResponse<>();

        response.setCode(200);
        response.setMessage("Activate user successfully");
        response.setData(userService.avtivateUser(id));

        return response;
    }

//    // ================= UPLOAD NEW AVATAR =================
//    @PutMapping(
//            value = "/{userId}/avatar",
//            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
//    )
//    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'ANNOTATOR', 'REVIEWER')")
//    @Operation(
//            summary = "Upload new avatar",
//            description = "Upload new avatar"
//    )
//    public ApiResponse<UserResponse> uploadAvatar(
//            @PathVariable String userId,
//            @RequestPart("file") MultipartFile file) throws Exception {
//        ApiResponse<UserResponse> response = new ApiResponse<>();
//        response.setCode(200);
//        response.setMessage("Avatar user upload successfully");
//        response.setData(userService.uploadAvatar(userId, file));
//        return response ;
//    }
//
//    // ================= CHANGE AVATAR =================
//    @PutMapping(
//            value = "/{userId}/avatar/edit",
//            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
//    )
//    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'ANNOTATOR', 'REVIEWER')")
//    @Operation(
//            summary = "Change avatar",
//            description = "Change avatar"
//    )
//    public ApiResponse<UserResponse> editAvatar(
//            @PathVariable String userId,
//            @RequestPart("file") MultipartFile file) throws Exception {
//        ApiResponse<UserResponse> response = new ApiResponse<>();
//        response.setCode(200);
//        response.setMessage("Avatar user edit successfully");
//        response.setData(userService.editAvatar(userId, file));
//        return response ;
//    }
//
//    // ================= REMOVE AVATAR =================
//    @DeleteMapping("/{userId}/avatar/remove")
//    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'ANNOTATOR', 'REVIEWER')")
//    @Operation(
//            summary = "Remove current user avatar",
//            description = "Remove current user avatar"
//    )
//    public ApiResponse<UserResponse> deleteAvatar(
//            @PathVariable String userId) throws Exception {
//        ApiResponse<UserResponse> response = new ApiResponse<>();
//        response.setCode(200);
//        response.setMessage("Avatar user delete successfully");
//        response.setData(userService.deleteAvatar(userId));
//        return response ;
//    }
}
