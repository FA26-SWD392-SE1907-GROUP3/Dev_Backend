package com.example.swd392_se1907_aives.domain.entity;

import com.example.swd392_se1907_aives.domain.enums.BloomLevel;
import com.example.swd392_se1907_aives.domain.enums.QuestionSource;
import com.example.swd392_se1907_aives.domain.enums.QuestionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Integer questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = true)
    private SubjectDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "topic", length = 150)
    private String topic;

    @Column(name = "question_content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String questionContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "bloom_level", nullable = false, length = 50)
    private BloomLevel bloomLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private QuestionSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private QuestionStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}