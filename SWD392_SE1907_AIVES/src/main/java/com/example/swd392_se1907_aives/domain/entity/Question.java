package com.example.swd392_se1907_aives.domain.entity;

import com.example.swd392_se1907_aives.domain.enums.BloomLevel;
import com.example.swd392_se1907_aives.domain.enums.QuestionSource;
import com.example.swd392_se1907_aives.domain.enums.QuestionStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "QuestionID")
    private Integer questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SubjectID", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DocumentID", nullable = true)
    private SubjectDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false)
    private User createdBy;

    @Column(name = "Topic", length = 150)
    private String topic;

    @Column(name = "QuestionContent", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String questionContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "BloomLevel", nullable = false, length = 50)
    private BloomLevel bloomLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "Source", nullable = false, length = 50)
    private QuestionSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false, length = 50)
    private QuestionStatus status;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}