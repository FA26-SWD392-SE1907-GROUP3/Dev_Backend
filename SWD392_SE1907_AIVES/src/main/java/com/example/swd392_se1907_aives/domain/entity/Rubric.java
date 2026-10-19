package com.example.swd392_se1907_aives.domain.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="rubrics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Rubric {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="course_id", nullable=false)
    private UUID courseId;
    @Column(name="created_by", nullable=false)
    private UUID createdBy;
    @Column(name="name", nullable=false, length=180)
    private String name;
    @Column(name="description", nullable=true, columnDefinition="text")
    private String description;
    @Column(name="max_score", nullable=false, precision=6, scale=2)
    private BigDecimal maxScore;
    @Column(name="status", nullable=false, length=20)
    private String status;
    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;
    @Column(name="updated_at", nullable=false)
    private OffsetDateTime updatedAt;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="course_id", insertable=false, updatable=false)
    private Course courseIdRef;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="created_by", insertable=false, updatable=false)
    private User createdByRef;
}
