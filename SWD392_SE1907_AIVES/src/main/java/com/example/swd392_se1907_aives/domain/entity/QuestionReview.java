package com.example.swd392_se1907_aives.domain.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="question_reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuestionReview {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="question_id", nullable=false)
    private UUID questionId;
    @Column(name="reviewer_id", nullable=false)
    private UUID reviewerId;
    @Column(name="decision", nullable=false, length=20)
    private String decision;
    @Column(name="comment", nullable=true)
    private String comment;
    @Column(name="reviewed_at", nullable=false)
    private OffsetDateTime reviewedAt;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="question_id", insertable=false, updatable=false)
    private Question questionIdRef;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="reviewer_id", insertable=false, updatable=false)
    private User reviewerIdRef;
}
