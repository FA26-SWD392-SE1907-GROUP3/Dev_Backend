package com.example.swd392_se1907_aives.service;
import com.example.swd392_se1907_aives.domain.entity.*;
import com.example.swd392_se1907_aives.domain.enums.*;
import com.example.swd392_se1907_aives.repository.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
@Component
@RequiredArgsConstructor
public class BootstrapService implements ApplicationRunner {
 private final RoleRepository roles; private final UserRepository users; private final PasswordEncoder encoder;
 @Value("${aives.bootstrap.username}") private String username;
 @Value("${aives.bootstrap.password}") private String password;
 @Value("${aives.bootstrap.email}") private String email;
 @Override @Transactional public void run(ApplicationArguments args) {
  for (RoleName name : RoleName.values()) if (roles.findByRoleName(name).isEmpty()) roles.save(Role.builder().roleName(name).build());
  if (!username.isBlank() && !users.existsByUsername(username)) {
   if (password.length() < 8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) throw new IllegalStateException("ADMIN_PASSWORD must contain at least 8 characters and at most 72 UTF-8 bytes");
   users.save(User.builder().username(username).passwordHash(encoder.encode(password)).fullName("Administrator")
     .email(email).role(roles.findByRoleName(RoleName.ADMIN).orElseThrow()).userStatus(UserStatus.ACTIVE).build());
  }
 }
}
