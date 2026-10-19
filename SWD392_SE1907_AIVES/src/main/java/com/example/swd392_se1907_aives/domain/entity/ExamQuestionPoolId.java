package com.example.swd392_se1907_aives.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class ExamQuestionPoolId implements Serializable {
    @Column(name="exam_id", nullable=false)
    private UUID examId;

    @Column(name="question_id", nullable=false)
    private UUID questionId;

}
