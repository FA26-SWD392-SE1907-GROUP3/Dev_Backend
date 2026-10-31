package com.example.swd392_se1907_aives.modules.examination.internal.domain.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="exam_candidates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamCandidate {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="exam_id", nullable=false)
    private UUID examId;
    @Column(name="course_id", nullable=false)
    private UUID courseId;
    @Column(name="student_id", nullable=false)
    private UUID studentId;
    @Column(name="scheduled_start", nullable=false)
    private OffsetDateTime scheduledStart;
    @Column(name="scheduled_end", nullable=false)
    private OffsetDateTime scheduledEnd;
    @Column(name="status", nullable=false, length=20)
    private String status;
    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;

}
