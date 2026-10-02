package com.example.swd392_se1907_aives.domain.entity;

import com.example.swd392_se1907_aives.domain.enums.ExamStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ExamSessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ExamID")
    private Integer examId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SubjectID", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy", nullable = false)
    private User createdBy;

    @Column(name = "ExamName", nullable = false, length = 200)
    private String examName;

    @Column(name = "StartTime", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "EndTime", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "MaxMainQuestions", nullable = false)
    private Integer maxMainQuestions;

    @Column(name = "MaxFollowUpQuestions", nullable = false)
    private Integer maxFollowUpQuestions;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", length = 20)
    private ExamStatus status;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = ExamStatus.DRAFT;
        }
    }
}