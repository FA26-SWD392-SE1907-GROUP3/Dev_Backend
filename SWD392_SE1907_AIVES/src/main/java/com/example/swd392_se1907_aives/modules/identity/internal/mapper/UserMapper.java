package com.example.swd392_se1907_aives.modules.identity.internal.mapper;

import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.response.UserResponse;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.entity.User;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {
    public UserResponse toUserResponse(User user) {
        return UserResponse.from(user);
    }

    public List<UserResponse> toUserResponses(List<User> users) {
        return users.stream().map(UserResponse::from).toList();
    }
}
