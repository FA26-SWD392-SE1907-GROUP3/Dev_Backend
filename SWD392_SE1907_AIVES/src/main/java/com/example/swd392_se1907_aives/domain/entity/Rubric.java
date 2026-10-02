package com.example.swd392_se1907_aives.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Rubrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rubric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RubricID")
    private Integer rubricId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "QuestionID", nullable = false)
    private Question question;

    @Column(name = "CriteriaName", nullable = false, length = 255)
    private String criteriaName;

    @Column(name = "MaxScore", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "Guideline", columnDefinition = "NVARCHAR(MAX)")
    private String guideline;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}