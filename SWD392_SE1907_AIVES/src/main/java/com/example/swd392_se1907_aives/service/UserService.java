package com.example.swd392_se1907_aives.service;
import com.example.swd392_se1907_aives.domain.dto.request.*;
import com.example.swd392_se1907_aives.domain.dto.response.UserResponse;
import com.example.swd392_se1907_aives.domain.entity.User;
import com.example.swd392_se1907_aives.mapper.UserMapper;
import com.example.swd392_se1907_aives.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.OffsetDateTime;
@Service @RequiredArgsConstructor
public class UserService {
    private final UserRepository users;
    private final UserMapper mapper;
    private final PasswordEncoder encoder;
    public List<UserResponse> getAllUsers(){ return mapper.toUserResponses(users.findAll()); }
    public UserResponse getUserById(UUID id){ return mapper.toUserResponse(get(id)); }
    private User get(UUID id){ return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"User not found")); }
    @Transactional public UserResponse createUser(UserCreationRequest req){
        String email=req.getEmail().trim().toLowerCase(Locale.ROOT);
        if(users.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already exists");
        User u=new User(); u.setFullName(req.getFullName().trim());u.setEmail(email);
        u.setRole(req.getRole().name());u.setIsActive(true);
        u.setPasswordHash(encoder.encode(req.getPassword()));
        var now=OffsetDateTime.now();u.setCreatedAt(now);u.setUpdatedAt(now);
        return mapper.toUserResponse(users.saveAndFlush(u));
    }
    @Transactional public UserResponse updateUser(UUID id, UserUpdateRequest req){
        var u=get(id);
        if(req.getEmail()!=null){String email=req.getEmail().trim().toLowerCase(Locale.ROOT);
          if(!email.equals(u.getEmail()) && users.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Email already exists");
          u.setEmail(email);}
        if(req.getFullName()!=null)u.setFullName(req.getFullName().trim());
        User actor=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // Only an admin can change role, including their own role.
        if(req.getRole()!=null){
            if(!"ADMIN".equals(actor.getRole())) throw new AccessDeniedException("Only admin can change roles");
            u.setRole(req.getRole().name());
        }
        u.setUpdatedAt(OffsetDateTime.now());return mapper.toUserResponse(users.save(u));
    }
    @Transactional public UserResponse updateUserPassword(UUID id, UserPasswordChangeRequest req){
        User actor=(User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!actor.getId().equals(id))throw new AccessDeniedException("You can only change your own password");
        var u=get(id);
        if(!Boolean.TRUE.equals(u.getIsActive()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Inactive user");
        if(!encoder.matches(req.getOldPassword(),u.getPasswordHash())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Old password incorrect");
        if(!req.getNewPassword().equals(req.getConfirmPassword())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Passwords do not match");
        if(encoder.matches(req.getNewPassword(),u.getPasswordHash()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"New password equals old password");
        u.setPasswordHash(encoder.encode(req.getNewPassword()));u.setUpdatedAt(OffsetDateTime.now());return mapper.toUserResponse(users.save(u));
    }
    @Transactional public UserResponse setActive(UUID id,boolean active){
        User actor=(User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(!active && actor.getId().equals(id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Không thể xóa hoặc khóa tài khoản đang đăng nhập.");
        var u=get(id);u.setIsActive(active);u.setUpdatedAt(OffsetDateTime.now());return mapper.toUserResponse(users.save(u));
    }
}
