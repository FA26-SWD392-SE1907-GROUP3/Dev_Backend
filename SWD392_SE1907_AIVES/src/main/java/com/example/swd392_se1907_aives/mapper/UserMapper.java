package com.example.swd392_se1907_aives.mapper;

import java.util.List;

import com.example.swd392_se1907_aives.domain.dto.request.UserCreationRequest;
import com.example.swd392_se1907_aives.domain.dto.request.UserPasswordChangeRequest;
import com.example.swd392_se1907_aives.domain.dto.request.UserUpdateRequest;
import com.example.swd392_se1907_aives.domain.dto.response.UserResponse;
import com.example.swd392_se1907_aives.domain.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toUserResponse(User user);
    List<UserResponse> toUserResponses(List<User> users);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User toUser(UserCreationRequest request);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "password", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromRequest(UserUpdateRequest request, @MappingTarget User user);

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "coverImage", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "userStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateUserPasswordFromRequest(UserPasswordChangeRequest request, @MappingTarget User user);
}

