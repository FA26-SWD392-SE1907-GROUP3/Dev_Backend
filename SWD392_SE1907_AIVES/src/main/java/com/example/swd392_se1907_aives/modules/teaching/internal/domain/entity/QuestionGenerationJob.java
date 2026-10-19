package com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="question_generation_jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuestionGenerationJob {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="course_id", nullable=false)
    private UUID courseId;
    @Column(name="requested_by", nullable=false)
    private UUID requestedBy;
    @Column(name="instruction", nullable=true)
    private String instruction;
    @Column(name="desired_count", nullable=false)
    private Integer desiredCount;
    @Column(name="target_bloom_level", nullable=true, length=20)
    private String targetBloomLevel;
    @Column(name="model_name", nullable=true, length=120)
    private String modelName;
    @Column(name="status", nullable=false, length=20)
    private String status;
    @Column(name="generated_count", nullable=false)
    private Integer generatedCount;
    @Column(name="error_message", nullable=true)
    private String errorMessage;
    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;
    @Column(name="completed_at", nullable=true)
    private OffsetDateTime completedAt;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="course_id", insertable=false, updatable=false)
    private Course courseIdRef;

}
