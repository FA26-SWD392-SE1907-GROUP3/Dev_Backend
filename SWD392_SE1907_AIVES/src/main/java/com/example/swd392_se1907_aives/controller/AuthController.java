package com.example.swd392_se1907_aives.controller;


import com.example.swd392_se1907_aives.domain.dto.request.AuthRequest;
import com.example.swd392_se1907_aives.domain.dto.response.ApiResponse;
import com.example.swd392_se1907_aives.domain.dto.response.AuthResponse;
import com.example.swd392_se1907_aives.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE , makeFinal = true)
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication management endpoints")
public class AuthController {

    AuthService authService;

    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticate user with email and password. Returns JWT token for subsequent API calls."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Invalid credentials or user not active"
            )
    })
    public ApiResponse<AuthResponse> login(@RequestBody AuthRequest request) {
        return ApiResponse.<AuthResponse>builder()
                .data(authService.authenticate(request))
                .build();
    }
}
