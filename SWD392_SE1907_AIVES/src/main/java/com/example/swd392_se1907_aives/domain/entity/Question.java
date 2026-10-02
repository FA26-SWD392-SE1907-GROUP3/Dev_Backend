package com.example.swd392_se1907_aives.domain.entity;

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
    @JoinColumn(name = "DocumentID", nullable = true) // NULL nếu giảng viên tự tạo thủ công
    private SubjectDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false)
    private User createdBy;

    @Column(name = "Topic", length = 150)
    private String topic;

    @Column(name = "QuestionContent", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String questionContent;

    @Column(name = "BloomLevel", nullable = false, length = 50)
    private String bloomLevel;

    @Column(name = "Source", nullable = false, length = 50)
    private String source;

    @Column(name = "Status", nullable = false, length = 50)
    private String status;

    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}