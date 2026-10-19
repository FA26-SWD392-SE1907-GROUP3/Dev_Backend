package com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.response;

import com.example.swd392_se1907_aives.modules.identity.internal.domain.entity.User;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String role;
    private boolean active;
    private OffsetDateTime createdAt;
    public static UserResponse from(User u){
        return UserResponse.builder().id(u.getId()).fullName(u.getFullName()).email(u.getEmail())
            .role(u.getRole()).active(Boolean.TRUE.equals(u.getIsActive())).createdAt(u.getCreatedAt()).build();
    }
}
