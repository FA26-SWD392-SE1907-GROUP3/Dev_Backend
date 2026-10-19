package com.example.swd392_se1907_aives.modules.examination.internal.repository;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.InterviewTurn;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface InterviewTurnRepository extends JpaRepository<InterviewTurn, UUID> {


    java.util.List<InterviewTurn> findByAttemptQuestionIdOrderByTurnNo(UUID attemptQuestionId);
    long countByAttemptQuestionIdInAndKind(java.util.Collection<UUID> questionIds,String kind);
    interface Progress {
        UUID getAttemptQuestionId(); Integer getTurnNo(); String getSpeaker(); String getKind(); String getSpeechStatus();
        java.time.OffsetDateTime getResponseDeadlineAt(); java.time.OffsetDateTime getCreatedAt();
    }
    java.util.List<Progress> findProgressByAttemptQuestionIdIn(java.util.Collection<UUID> questionIds);
}
