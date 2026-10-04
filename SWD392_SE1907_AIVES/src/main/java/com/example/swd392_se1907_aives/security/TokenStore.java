package com.example.swd392_se1907_aives.security;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
@Component
public class TokenStore {
 private record Session(Integer userId, Instant expires) {}
 private final Map<String, Session> sessions = new ConcurrentHashMap<>();
 public String issue(Integer userId) {
  sessions.entrySet().removeIf(e -> e.getValue().expires().isBefore(Instant.now()));
  byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
  String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  sessions.put(token, new Session(userId, Instant.now().plusSeconds(3600)));
  return token;
 }
 public Integer userId(String token) {
  Session session = sessions.get(token);
  if (session == null) return null;
  if (!session.expires().isAfter(Instant.now())) { sessions.remove(token); return null; }
  return session.userId();
 }
 public void revoke(String token) { sessions.remove(token); }
 public void revokeUser(Integer id) { sessions.entrySet().removeIf(e -> e.getValue().userId().equals(id)); }
}
