package com.example.swd392_se1907_aives.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Component
public class GoogleTokenVerifier {
 private final String clientId;
 private final JwtDecoder decoder;
 @org.springframework.beans.factory.annotation.Autowired
 public GoogleTokenVerifier(@Value("${aives.google.client-id:}") String clientId) {
  this(clientId,NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build());
 }
 GoogleTokenVerifier(String clientId,NimbusJwtDecoder jwtDecoder) {
  this.clientId=clientId.trim();
  jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(),
   new JwtClaimValidator<String>("iss", v -> "https://accounts.google.com".equals(v) || "accounts.google.com".equals(v)),
   new JwtClaimValidator<List<String>>("aud", v -> v!=null && v.contains(this.clientId)),
   new JwtClaimValidator<Boolean>("email_verified", Boolean.TRUE::equals),
   new JwtClaimValidator<String>("sub", v -> v!=null && !v.isBlank() && v.length()<=255),
   new JwtClaimValidator<Object>("exp", Objects::nonNull)));
  decoder=jwtDecoder;
 }
 public String clientId() { return clientId; }
 public Jwt verify(String credential,String nonce) {
  if(clientId.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Đăng nhập Google chưa được cấu hình.");
  try {
   var jwt=decoder.decode(credential);
   if(!nonce.equals(jwt.getClaimAsString("nonce"))) throw new JwtException("Nonce mismatch");
   return jwt;
  } catch(JwtException | IllegalArgumentException ex) {
   throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Không xác thực được tài khoản Google. Vui lòng thử lại.");
  }
 }
}
