package com.example.swd392_se1907_aives.modules.examination.internal.domain.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;
@Entity
@DynamicInsert
@Table(name="ai_interview_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiInterviewRequest {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="attempt_question_id", nullable=false)
    private UUID attemptQuestionId;
    @Column(name="input_turn_id", nullable=true)
    private UUID inputTurnId;
    @Column(name="output_turn_id", nullable=true)
    private UUID outputTurnId;
    @Column(name="request_type", nullable=false, length=25)
    private String requestType;
    @Column(name="provider", nullable=true, length=80)
    private String provider;
    @Column(name="model_name", nullable=true, length=120)
    private String modelName;
    @Column(name="status", nullable=false, length=20)
    private String status;
    @Column(name="latency_ms", nullable=true)
    private Integer latencyMs;
    @Column(name="input_tokens", nullable=true)
    private Integer inputTokens;
    @Column(name="output_tokens", nullable=true)
    private Integer outputTokens;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="parameters", nullable=false)
    private JsonNode parameters;
    @Column(name="error_message", nullable=true)
    private String errorMessage;
    @Column(name="requested_at", nullable=false)
    private OffsetDateTime requestedAt;
    @Column(name="finished_at", nullable=true)
    private OffsetDateTime finishedAt;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="attempt_question_id", insertable=false, updatable=false)
    private AttemptQuestion attemptQuestionIdRef;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="input_turn_id", insertable=false, updatable=false)
    private InterviewTurn inputTurnIdRef;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="output_turn_id", insertable=false, updatable=false)
    private InterviewTurn outputTurnIdRef;
}
