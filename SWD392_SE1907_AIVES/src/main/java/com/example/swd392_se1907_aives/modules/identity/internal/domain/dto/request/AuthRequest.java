package com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request;


import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AuthRequest {
    @Email @NotBlank private String email;
    @NotBlank private String password;
}
