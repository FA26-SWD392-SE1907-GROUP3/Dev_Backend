package com.example.swd392_se1907_aives.modules.identity.internal.service.impl;

import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request.UserCreationRequest;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request.UserPasswordChangeRequest;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request.UserUpdateRequest;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.response.UserResponse;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.entity.User;
import com.example.swd392_se1907_aives.modules.identity.internal.mapper.UserMapper;
import com.example.swd392_se1907_aives.modules.identity.internal.repository.UserRepository;
import com.example.swd392_se1907_aives.modules.identity.internal.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository users;
    private final UserMapper mapper;
    private final PasswordEncoder encoder;

    @Override
    public List<UserResponse> getAllUsers() {
        return mapper.toUserResponses(users.findAll());
    }

    @Override
    public UserResponse getUserById(UUID id) {
        return mapper.toUserResponse(get(id));
    }

    private User get(UUID id) {
        return users.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @Override
    @Transactional
    public UserResponse createUser(UserCreationRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        User u = new User();
        u.setFullName(request.getFullName().trim());
        u.setEmail(email);
        u.setRole(request.getRole().name());
        u.setIsActive(true);
        u.setPasswordHash(encoder.encode(request.getPassword()));
        var now = OffsetDateTime.now();
        u.setCreatedAt(now);
        u.setUpdatedAt(now);
        return mapper.toUserResponse(users.saveAndFlush(u));
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UserUpdateRequest request) {
        var u = get(id);
        if (request.getEmail() != null) {
            String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!email.equals(u.getEmail()) && users.existsByEmailIgnoreCase(email))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
            u.setEmail(email);
        }
        if (request.getFullName() != null) u.setFullName(request.getFullName().trim());
        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // Only an admin can change role, including their own role.
        if (request.getRole() != null) {
            if (!"ADMIN".equals(actor.getRole()))
                throw new AccessDeniedException("Only admin can change roles");
            u.setRole(request.getRole().name());
        }
        u.setUpdatedAt(OffsetDateTime.now());
        return mapper.toUserResponse(users.save(u));
    }

    @Override
    @Transactional
    public UserResponse updateUserPassword(UUID id, UserPasswordChangeRequest request) {
        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!actor.getId().equals(id))
            throw new AccessDeniedException("You can only change your own password");
        var u = get(id);
        if (!Boolean.TRUE.equals(u.getIsActive()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inactive user");
        if (!encoder.matches(request.getOldPassword(), u.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Old password incorrect");
        if (!request.getNewPassword().equals(request.getConfirmPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        if (encoder.matches(request.getNewPassword(), u.getPasswordHash()))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "New password equals old password");
        u.setPasswordHash(encoder.encode(request.getNewPassword()));
        u.setUpdatedAt(OffsetDateTime.now());
        return mapper.toUserResponse(users.save(u));
    }

    @Override
    @Transactional
    public UserResponse setActive(UUID id, boolean active) {
        User actor = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!active && actor.getId().equals(id))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Không thể xóa hoặc khóa tài khoản đang đăng nhập.");
        var u = get(id);
        u.setIsActive(active);
        u.setUpdatedAt(OffsetDateTime.now());
        return mapper.toUserResponse(users.save(u));
    }
}
