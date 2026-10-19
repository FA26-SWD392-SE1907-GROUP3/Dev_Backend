package com.example.swd392_se1907_aives.domain.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="exam_question_pool")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamQuestionPool {
    @EmbeddedId
    private ExamQuestionPoolId id;
    @Column(name="course_id", nullable=false)
    private UUID courseId;
    @Column(name="selection_weight", nullable=false)
    private Integer selectionWeight;
    @Column(name="added_at", nullable=false)
    private OffsetDateTime addedAt;
}
