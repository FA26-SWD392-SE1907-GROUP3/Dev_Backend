package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.Rubric;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface RubricRepository extends JpaRepository<Rubric, UUID> {
    java.util.List<Rubric> findByCourseId(UUID courseId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select r from Rubric r where r.id = :id")
    java.util.Optional<Rubric> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);



}
