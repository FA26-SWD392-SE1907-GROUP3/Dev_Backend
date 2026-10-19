package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.ExamCandidate;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ExamCandidateRepository extends JpaRepository<ExamCandidate, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from ExamCandidate c where c.id = :id")
    java.util.Optional<ExamCandidate> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);

    java.util.List<ExamCandidate> findByStudentId(UUID studentId);
    boolean existsByExamIdAndStudentId(UUID examId, UUID studentId);
    java.util.List<ExamCandidate> findByExamIdOrderByScheduledStartAsc(UUID examId);
    @org.springframework.data.jpa.repository.Query("select c.examId from ExamCandidate c where c.id = :id")
    java.util.Optional<UUID> findExamIdById(@org.springframework.data.repository.query.Param("id") UUID id);

}
