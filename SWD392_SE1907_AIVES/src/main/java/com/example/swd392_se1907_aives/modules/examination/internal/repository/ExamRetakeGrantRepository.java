package com.example.swd392_se1907_aives.modules.examination.internal.repository;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamRetakeGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamRetakeGrantRepository extends JpaRepository<ExamRetakeGrant, UUID> {
    Optional<ExamRetakeGrant> findFirstByCandidateIdAndConsumedAtIsNullAndRevokedAtIsNullOrderByGrantedAtDesc(UUID candidateId);
    List<ExamRetakeGrant> findByCandidateIdOrderByGrantedAtAsc(UUID candidateId);
}
