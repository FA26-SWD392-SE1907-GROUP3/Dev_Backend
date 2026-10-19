package com.example.swd392_se1907_aives.modules.examination.internal.repository;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.AiInterviewRequest;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface AiInterviewRequestRepository extends JpaRepository<AiInterviewRequest,UUID> {
    Optional<AiInterviewRequest> findFirstByInputTurnIdAndRequestTypeOrderByRequestedAtDesc(UUID turnId,String type);
    interface Progress {UUID getAttemptQuestionId();String getRequestType();String getStatus();java.time.OffsetDateTime getRequestedAt();}
    List<Progress> findProgressByAttemptQuestionIdIn(Collection<UUID> questionIds);
}
