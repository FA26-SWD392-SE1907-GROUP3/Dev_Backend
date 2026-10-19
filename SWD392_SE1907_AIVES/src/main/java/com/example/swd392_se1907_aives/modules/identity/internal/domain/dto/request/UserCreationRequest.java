package com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request;

import com.example.swd392_se1907_aives.modules.identity.internal.domain.enums.RoleName;

import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserCreationRequest {
    @NotBlank @Size(max=150) private String fullName;
    @NotBlank @Email @Size(max=255) private String email;
    @NotBlank @Size(min=8,max=100) private String password;
    @NotNull private RoleName role;
}
