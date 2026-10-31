package com.example.swd392_se1907_aives.modules.examination.internal.domain.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="attempt_questions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttemptQuestion {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="attempt_id", nullable=false)
    private UUID attemptId;
    @Column(name="exam_id", nullable=false)
    private UUID examId;
    @Column(name="question_id", nullable=false)
    private UUID questionId;
    @Column(name="sequence_no", nullable=false)
    private Integer sequenceNo;
    @Column(name="question_text_snapshot", nullable=false, columnDefinition="text")
    private String questionTextSnapshot;
    @Column(name="reference_answer_snapshot", columnDefinition="TEXT")
    private String referenceAnswerSnapshot;
    @Column(name="rubric_snapshot", columnDefinition="TEXT")
    private String rubricSnapshot;
    @Column(name="selection_mode", nullable=false, length=15)
    private String selectionMode;
    @Column(name="selection_reason", nullable=true)
    private String selectionReason;
    @Column(name="status", nullable=false, length=20)
    private String status;
    @Column(name="started_at", nullable=true)
    private OffsetDateTime startedAt;
    @Column(name="completed_at", nullable=true)
    private OffsetDateTime completedAt;
    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;
}
