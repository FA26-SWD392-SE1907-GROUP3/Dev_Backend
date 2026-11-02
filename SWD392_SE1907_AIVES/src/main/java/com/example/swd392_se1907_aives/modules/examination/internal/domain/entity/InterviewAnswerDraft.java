package com.example.swd392_se1907_aives.modules.examination.internal.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Text checkpoints only; never contains microphone recordings. */
@Entity
@Table(name = "interview_answer_drafts")
@Getter @Setter
public class InterviewAnswerDraft {
    @Id @Column(name = "prompt_id", nullable = false)
    private UUID promptId;
    @Column(name = "attempt_id", nullable = false)
    private UUID attemptId;
    @Column(name = "revision", nullable = false)
    private long revision;
    @Column(name = "text_content", nullable = false, columnDefinition = "text")
    private String text;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
