package com.example.swd392_se1907_aives.domain.entity;

import com.example.swd392_se1907_aives.domain.enums.QuestionType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "InterviewLogs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LogID")
    private Integer logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ScheduleID", nullable = false)
    private ExamSchedule examSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "QuestionID", nullable = true)
    private Question question;

    @Column(name = "QuestionContent", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String questionContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "QuestionType", nullable = false, length = 20)
    private QuestionType questionType;

    @Column(name = "QuestionAudioURL", length = 500)
    private String questionAudioUrl;

    @Column(name = "StudentAnswerTranscript", columnDefinition = "NVARCHAR(MAX)")
    private String studentAnswerTranscript;

    @Column(name = "AnswerAudioURL", length = 500)
    private String answerAudioUrl;

    @Column(name = "AskedAt", nullable = false)
    private LocalDateTime askedAt;

    @Column(name = "AnsweredAt")
    private LocalDateTime answeredAt;

    @Column(name = "TimeTakenSeconds")
    private Integer timeTakenSeconds;
}