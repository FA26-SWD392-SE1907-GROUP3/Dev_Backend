package com.example.swd392_se1907_aives.modules.examination.internal.domain.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="interview_turns")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewTurn {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="attempt_question_id", nullable=false)
    private UUID attemptQuestionId;
    @Column(name="turn_no", nullable=false)
    private Integer turnNo;
    @Column(name="speaker", nullable=false, length=10)
    private String speaker;
    @Column(name="kind", nullable=false, length=25)
    private String kind;
    @Column(name="followup_number", nullable=false)
    private Integer followupNumber;
    @Column(name="reply_to_turn_id", nullable=true)
    private UUID replyToTurnId;
    @Column(name="text_content", nullable=true)
    private String textContent;
    @Column(name="audio_object_key", nullable=true)
    private String audioObjectKey;
    @Column(name="speech_status", nullable=false, length=20)
    private String speechStatus;
    @Column(name="stt_confidence", nullable=true, precision=5, scale=4)
    private BigDecimal sttConfidence;
    @Column(name="ai_model_name", nullable=true, length=120)
    private String aiModelName;
    @Column(name="followup_reason", nullable=true)
    private String followupReason;
    @Column(name="response_deadline_at", nullable=true)
    private OffsetDateTime responseDeadlineAt;
    @Column(name="started_at", nullable=true)
    private OffsetDateTime startedAt;
    @Column(name="ended_at", nullable=true)
    private OffsetDateTime endedAt;
    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="attempt_question_id", insertable=false, updatable=false)
    private AttemptQuestion attemptQuestionIdRef;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="reply_to_turn_id", insertable=false, updatable=false)
    private InterviewTurn replyToTurnIdRef;
}
