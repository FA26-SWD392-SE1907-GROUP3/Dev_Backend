package com.example.swd392_se1907_aives.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UserPasswordChangeRequest {
    @NotBlank
    String oldPassword;

    @NotBlank
    @Size(min = 8, message = "INVALID_PASSWORD_LENGTH")
    String newPassword;

    @NotBlank
    String confirmPassword;
}
