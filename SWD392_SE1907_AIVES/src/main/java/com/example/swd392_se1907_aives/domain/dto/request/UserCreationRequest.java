package com.example.swd392_se1907_aives.domain.dto.request;

import com.example.swd392_se1907_aives.domain.enums.RoleName;
import com.example.swd392_se1907_aives.domain.enums.UserStatus;

import jakarta.validation.constraints.Email;
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
public class UserCreationRequest {
    @Size(min = 3, max = 50, message = "INVALID_USERNAME_LENGTH")
    String username;

    @Size(min = 3, max = 50, message = "INVALID_USERNAME_LENGTH")
    String fullName;

    @Email(message = "INVALID_EMAIL_FORMAT")
    String email;

    @Size(min = 8, message = "INVALID_PASSWORD_LENGTH")
    String password;

    RoleName role;

    UserStatus userStatus;
}
