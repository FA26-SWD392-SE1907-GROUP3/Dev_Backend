package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RoleRepository extends JpaRepository<Role, Integer> { Optional<Role> findByRoleName(com.example.swd392_se1907_aives.domain.enums.RoleName roleName); }
