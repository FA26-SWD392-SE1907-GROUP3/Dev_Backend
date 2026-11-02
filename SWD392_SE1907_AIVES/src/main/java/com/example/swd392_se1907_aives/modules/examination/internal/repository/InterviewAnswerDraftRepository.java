package com.example.swd392_se1907_aives.modules.examination.internal.repository;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.InterviewAnswerDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface InterviewAnswerDraftRepository extends JpaRepository<InterviewAnswerDraft, UUID> {}
