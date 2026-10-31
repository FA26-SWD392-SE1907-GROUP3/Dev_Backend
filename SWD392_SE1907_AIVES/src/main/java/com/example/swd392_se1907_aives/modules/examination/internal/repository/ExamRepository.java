package com.example.swd392_se1907_aives.modules.examination.internal.repository;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.Exam;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ExamRepository extends JpaRepository<Exam, UUID> {
    boolean existsByRubricId(UUID rubricId);
    java.util.List<Exam> findByCourseIdIn(java.util.Collection<UUID> courseIds);
    boolean existsByCourseIdAndStatusIn(UUID courseId,java.util.Collection<String> statuses);
    @org.springframework.data.jpa.repository.Query("select e.courseId from Exam e where e.id = :id")
    java.util.Optional<UUID> findCourseIdById(@org.springframework.data.repository.query.Param("id") UUID id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Exam e where e.id = :id")
    java.util.Optional<Exam> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);
}
