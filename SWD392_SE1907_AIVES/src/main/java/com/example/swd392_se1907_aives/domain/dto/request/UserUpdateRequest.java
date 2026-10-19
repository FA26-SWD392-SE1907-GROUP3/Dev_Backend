package com.example.swd392_se1907_aives.domain.dto.request;
import jakarta.validation.constraints.*;
import lombok.*;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UserUpdateRequest {
    @Size(max=150) private String fullName;
    @Email @Size(max=255) private String email;
    private com.example.swd392_se1907_aives.domain.enums.RoleName role;
}
