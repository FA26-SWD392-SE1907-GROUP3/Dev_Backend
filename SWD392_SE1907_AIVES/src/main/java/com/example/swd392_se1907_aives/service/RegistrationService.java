package com.example.swd392_se1907_aives.service;

import com.example.swd392_se1907_aives.domain.entity.User;
import com.example.swd392_se1907_aives.domain.enums.*;
import com.example.swd392_se1907_aives.dto.ApiModels.Registration;
import com.example.swd392_se1907_aives.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import java.util.*;

@Service @RequiredArgsConstructor
public class RegistrationService {
 private final UserRepository users; private final RoleRepository roles; private final PasswordEncoder encoder;
 @Transactional public User register(Registration input) {
  String email=input.email().trim().toLowerCase(Locale.ROOT);
  if(input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mật khẩu tối đa 72 byte UTF-8.");
  if(users.existsByUsername(input.username()) || users.existsByEmailIgnoreCase(email))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Tên đăng nhập hoặc email đã được sử dụng.");
  return users.saveAndFlush(student(input.username(),input.fullName().trim(),email,input.password()));
 }
 private User student(String username,String name,String email,String password) {
  if(name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Vui lòng nhập họ tên.");
  return User.builder().username(username).fullName(name).email(email).passwordHash(encoder.encode(password))
   .userStatus(UserStatus.ACTIVE).role(roles.findByRoleName(RoleName.STUDENT).orElseThrow()).build();
 }
 @Transactional public User google(org.springframework.security.oauth2.jwt.Jwt jwt) {
  var existing=users.findByGoogleSubject(jwt.getSubject());
  if(existing.isPresent()) {
   var user=existing.get();
   if(user.getUserStatus()!=UserStatus.ACTIVE) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Tài khoản đã bị vô hiệu hóa.");
   return user;
  }
  String email=jwt.getClaimAsString("email");
  if(email==null || email.length()>100 || !email.contains("@"))
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Email Google không phù hợp với tài khoản AIVES.");
  email=email.toLowerCase(Locale.ROOT);
  if(users.existsByEmailIgnoreCase(email))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Email đã có tài khoản AIVES. Vui lòng đăng nhập bằng mật khẩu của tài khoản đó.");
  String name=jwt.getClaimAsString("name");
  if(name==null || name.isBlank()) name=email.substring(0,email.indexOf('@'));
  var user=student("google_"+UUID.randomUUID().toString().replace("-",""),name.substring(0,Math.min(name.length(),100)),email,UUID.randomUUID().toString());
  user.setGoogleSubject(jwt.getSubject());
  return users.saveAndFlush(user);
 }
}
