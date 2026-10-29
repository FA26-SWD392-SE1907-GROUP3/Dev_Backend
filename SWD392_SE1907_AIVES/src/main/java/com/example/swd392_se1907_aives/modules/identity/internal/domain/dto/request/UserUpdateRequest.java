package com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request;

import com.example.swd392_se1907_aives.modules.identity.internal.domain.enums.RoleName;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    @Pattern(regexp = "(?s).*\\S.*", message = "Họ tên không được để trống")
    @Size(max = 150)
    private String fullName;

    @Pattern(regexp = "(?s).*\\S.*", message = "Email không được để trống")
    @Email
    @Size(max = 255)
    private String email;

    private RoleName role;
}
