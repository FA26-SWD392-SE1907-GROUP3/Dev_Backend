package com.example.swd392_se1907_aives.service;
import com.example.swd392_se1907_aives.domain.entity.*;
import com.example.swd392_se1907_aives.domain.enums.*;
import com.example.swd392_se1907_aives.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class AccessService {
 private final UserRepository users;
 public User current() {
  var auth = SecurityContextHolder.getContext().getAuthentication();
  if (auth == null || !(auth.getPrincipal() instanceof Integer id)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
 }
 public boolean admin() { return current().getRole().getRoleName() == RoleName.ADMIN; }
 public void owner(User owner) {
  if (!admin() && !current().getUserId().equals(owner.getUserId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
 }
 public void schedule(ExamSchedule schedule) {
  var me = current();
  if (me.getRole().getRoleName() == RoleName.STUDENT) owner(schedule.getStudent());
  else owner(schedule.getExamSession().getCreatedBy());
 }
}
