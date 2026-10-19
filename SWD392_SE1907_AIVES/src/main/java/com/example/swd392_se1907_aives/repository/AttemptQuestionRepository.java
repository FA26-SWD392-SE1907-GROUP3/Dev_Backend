package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.AttemptQuestion;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface AttemptQuestionRepository extends JpaRepository<AttemptQuestion, UUID> {
    java.util.List<AttemptQuestion> findByAttemptIdOrderBySequenceNo(UUID attemptId);
    interface Progress {
        UUID getId(); UUID getAttemptId(); Integer getSequenceNo(); String getStatus();
    }
    java.util.List<Progress> findProgressByAttemptIdIn(java.util.Collection<UUID> attemptIds);



}
