package com.example.swd392_se1907_aives.modules.examination.internal.repository;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamAttempt;


import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, UUID> {
    boolean existsByExamId(UUID examId);
    boolean existsByCandidateIdAndStatus(UUID candidateId,String status);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from ExamAttempt a where a.id = :id")
    java.util.Optional<ExamAttempt> findLockedById(@org.springframework.data.repository.query.Param("id") UUID id);
    List<ExamAttempt> findTop100ByStatusOrderByLastActivityAtAsc(String status);
    boolean existsByCandidateId(UUID candidateId);
    boolean existsByExamIdAndStatus(UUID examId,String status);
    java.util.Optional<ExamAttempt> findFirstByExamIdOrderByStartedAtDesc(UUID examId);
    List<ExamAttempt> findTop20ByExamIdOrderByStartedAtDesc(UUID examId);
    List<ExamAttempt> findByCandidateIdOrderByAttemptNoAsc(UUID candidateId);
    List<ExamAttempt> findByExamIdOrderByStartedAtDesc(UUID examId);
}
