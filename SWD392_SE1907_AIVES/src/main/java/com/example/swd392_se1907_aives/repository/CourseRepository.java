package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.Course;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface CourseRepository extends JpaRepository<Course, UUID> {
    boolean existsByCodeIgnoreCaseAndIdNot(String code,UUID id);
    boolean existsByCodeIgnoreCase(String code);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Course c where c.id = :id")
    java.util.Optional<Course> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);
}
