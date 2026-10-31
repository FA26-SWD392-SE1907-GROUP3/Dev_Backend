package com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="question_generation_materials")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuestionGenerationMaterial {
    @EmbeddedId
    private QuestionGenerationMaterialId id;
    @Column(name="course_id", nullable=false)
    private UUID courseId;
}
