package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserRepository extends JpaRepository<User, Integer> {
 @org.springframework.data.jpa.repository.EntityGraph(attributePaths="role") Optional<User> findByUsername(String username);
 @Override @org.springframework.data.jpa.repository.EntityGraph(attributePaths="role") Optional<User> findById(Integer id);
 boolean existsByUsername(String username); boolean existsByEmail(String email);
 @org.springframework.data.jpa.repository.EntityGraph(attributePaths="role") Optional<User> findByGoogleSubject(String subject);
 boolean existsByEmailIgnoreCase(String email);
}
