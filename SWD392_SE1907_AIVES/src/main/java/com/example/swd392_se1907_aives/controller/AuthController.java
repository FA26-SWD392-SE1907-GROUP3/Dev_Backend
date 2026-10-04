package com.example.swd392_se1907_aives.controller;
import com.example.swd392_se1907_aives.dto.ApiModels.Login;
import com.example.swd392_se1907_aives.repository.UserRepository;
import com.example.swd392_se1907_aives.domain.enums.UserStatus;
import com.example.swd392_se1907_aives.security.TokenStore;
import com.example.swd392_se1907_aives.service.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.*;
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
 private final UserRepository users; private final PasswordEncoder encoder; private final TokenStore tokens; private final AccessService access;
 private final RegistrationService registration; private final GoogleTokenVerifier googleVerifier;
 private final java.util.concurrent.ConcurrentHashMap<String,java.time.Instant> challenges=new java.util.concurrent.ConcurrentHashMap<>();
 private Map<String,Object> session(com.example.swd392_se1907_aives.domain.entity.User user) {
  return Map.of("token",tokens.issue(user.getUserId()),"user",Views.user(user),"expiresIn",3600);
 }
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
 public Map<String,Object> register(@Valid @RequestBody com.example.swd392_se1907_aives.dto.ApiModels.Registration input) {
  return session(registration.register(input));
 }
 @GetMapping("/google/config") public Map<String,Object> googleConfig() {
  return Map.of("clientId",googleVerifier.clientId(),"enabled",!googleVerifier.clientId().isBlank());
 }
 @GetMapping("/google/challenge") public synchronized Map<String,String> challenge() {
  if(googleVerifier.clientId().isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Đăng nhập Google chưa được cấu hình.");
  var now=java.time.Instant.now(); challenges.entrySet().removeIf(e -> e.getValue().isBefore(now));
  if(challenges.size()>=1000) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Vui lòng thử lại sau.");
  String nonce=UUID.randomUUID().toString(); challenges.put(nonce,now.plusSeconds(300)); return Map.of("nonce",nonce);
 }
 @PostMapping("/google") public Map<String,Object> google(@Valid @RequestBody com.example.swd392_se1907_aives.dto.ApiModels.GoogleLogin input) {
  var expires=challenges.remove(input.nonce());
  if(expires==null || expires.isBefore(java.time.Instant.now())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Phiên đăng nhập Google đã hết hạn. Vui lòng thử lại.");
  return session(registration.google(googleVerifier.verify(input.credential(),input.nonce())));
 }
 @PostMapping("/login") public Map<String,Object> login(@Valid @RequestBody Login input) {
  var user = users.findByUsername(input.username()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials"));
  if (user.getUserStatus() != UserStatus.ACTIVE || !encoder.matches(input.password(),user.getPasswordHash()))
    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials");
  return session(user);
 }
 @GetMapping("/me") public Map<String,Object> me() { return Views.user(access.current()); }
 @PostMapping("/logout") public void logout(@RequestHeader("Authorization") String header) { tokens.revoke(header.substring(7)); }
}
