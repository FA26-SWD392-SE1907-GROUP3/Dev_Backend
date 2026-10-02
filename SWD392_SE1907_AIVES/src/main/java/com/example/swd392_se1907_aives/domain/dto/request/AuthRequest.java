package com.example.swd392_se1907_aives.domain.dto.request;

import jakarta.validation.constraints.Email;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class AuthRequest {

    @Email(message = "INVALID_EMAIL_FORMAT")
    String email;
    String password;
}

