package com.example.swd392_se1907_aives.domain.dto.response;

import java.time.LocalDate;

import com.example.swd392_se1907_aives.domain.entity.Role;
import com.example.swd392_se1907_aives.domain.enums.UserStatus;

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
public class UserResponse {
    Integer userId;
    String username;
    String email;
    String coverImage;
    String specialization;
    Role role;
    UserStatus userStatus;
    LocalDate createdAt;
}