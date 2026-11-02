package com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.nio.charset.StandardCharsets;

@Getter @Setter
public class RegistrationRequest {
    @NotBlank(message="Họ tên không được để trống") @Size(max=150)
    private String fullName;
    @NotBlank @Email(message="Email không hợp lệ") @Size(max=255)
    private String email;
    public void setEmail(String value) { this.email=value==null?null:value.trim(); }
    @NotBlank @Size(min=8,max=72,message="Mật khẩu cần từ 8 đến 72 ký tự")
    private String password;
    @NotBlank(message="Vui lòng xác nhận mật khẩu")
    private String confirmPassword;

    @AssertTrue(message="Mật khẩu xác nhận không khớp")
    public boolean isPasswordConfirmed() { return password==null || confirmPassword==null || password.equals(confirmPassword); }
    @AssertTrue(message="Mật khẩu quá dài; tối đa 72 byte UTF-8")
    public boolean isPasswordWithinByteLimit() { return password==null || password.getBytes(StandardCharsets.UTF_8).length<=72; }
}
