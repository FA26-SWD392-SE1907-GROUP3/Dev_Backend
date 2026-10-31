package com.example.swd392_se1907_aives.modules.examination.internal.domain.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="exam_attempts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamAttempt {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="candidate_id", nullable=false)
    private UUID candidateId;
    @Column(name="exam_id", nullable=false)
    private UUID examId;
    @Column(name="attempt_no", nullable=false)
    private Integer attemptNo;
    @Column(name="status", nullable=false, length=20)
    private String status;
    @Column(name="started_at", nullable=true)
    private OffsetDateTime startedAt;
    @Column(name="ended_at", nullable=true)
    private OffsetDateTime endedAt;
    @Column(name="last_activity_at", nullable=true)
    private OffsetDateTime lastActivityAt;
    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;

    @Column(name="rubric_snapshot", columnDefinition="TEXT")
    private String rubricSnapshot;
    @Column(name="grading_status", length=20)
    private String gradingStatus;
    @Column(name="grading_error", columnDefinition="TEXT")
    private String gradingError;
    @Column(name="grading_result", columnDefinition="TEXT")
    private String gradingResult;
    @Column(name="grading_requested_at")
    private OffsetDateTime gradingRequestedAt;
    @Column(name="graded_at")
    private OffsetDateTime gradedAt;
}
