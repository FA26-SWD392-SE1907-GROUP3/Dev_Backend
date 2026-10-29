package com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="questions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Question {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="course_id", nullable=false)
    private UUID courseId;
    @Column(name="topic_id", nullable=true)
    private UUID topicId;
    @Column(name="created_by", nullable=false)
    private UUID createdBy;
    @Column(name="content", nullable=false, columnDefinition="text")
    private String content;
    @Column(name="reference_answer", nullable=true, columnDefinition="text")
    private String referenceAnswer;
    @Column(name="difficulty", nullable=false, length=15)
    private String difficulty;
    @Column(name="source_type", nullable=false, length=15)
    private String sourceType;
    @Column(name="generation_job_id", nullable=true)
    private UUID generationJobId;
    @Column(name="import_batch_id", nullable=true)
    private UUID importBatchId;
    @Column(name="status", nullable=false, length=20)
    private String status;
    @Column(name="approved_by", nullable=true)
    private UUID approvedBy;
    @Column(name="approved_at", nullable=true)
    private OffsetDateTime approvedAt;
    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;
    @Column(name="updated_at", nullable=false)
    private OffsetDateTime updatedAt;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="course_id", insertable=false, updatable=false)
    private Course courseIdRef;


}
