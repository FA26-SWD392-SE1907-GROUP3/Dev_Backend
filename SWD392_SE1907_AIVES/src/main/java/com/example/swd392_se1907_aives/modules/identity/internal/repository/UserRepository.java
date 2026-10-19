package com.example.swd392_se1907_aives.modules.identity.internal.repository;

import com.example.swd392_se1907_aives.modules.identity.internal.domain.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
