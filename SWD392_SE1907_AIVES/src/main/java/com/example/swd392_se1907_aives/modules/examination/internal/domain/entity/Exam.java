package com.example.swd392_se1907_aives.modules.examination.internal.domain.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="exams")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Exam {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="course_id", nullable=false)
    private UUID courseId;
    @Column(name="rubric_id")
    private UUID rubricId;
    @Column(name="created_by", nullable=false)
    private UUID createdBy;
    @Column(name="title", nullable=false, length=200)
    private String title;
    @Column(name="instructions", nullable=true)
    private String instructions;
    @Column(name="status", nullable=false, length=20)
    private String status;
    @Column(name="selection_mode", nullable=false, length=15)
    private String selectionMode;
    @Column(name="main_question_count", nullable=false)
    private Integer mainQuestionCount;
    @Column(name="max_followups_per_question", nullable=false)
    private Integer maxFollowupsPerQuestion;
    @Column(name="max_followups_per_student")
    private Integer maxFollowupsPerStudent;
    public int followupBudget(){return maxFollowupsPerStudent!=null?maxFollowupsPerStudent:maxFollowupsPerQuestion==null?0:maxFollowupsPerQuestion;}
    @Column(name="answer_time_limit_seconds", nullable=false)
    private Integer answerTimeLimitSeconds;
    @Column(name="slot_duration_minutes", nullable=false)
    private Integer slotDurationMinutes;
    @Column(name="exam_language", nullable=false, length=5)
    private String examLanguage;
    @Column(name="exam_timezone", nullable=false, length=80)
    private String examTimezone;
    @Column(name="starts_at", nullable=true)
    private OffsetDateTime startsAt;
    @Column(name="ends_at", nullable=true)
    private OffsetDateTime endsAt;
    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;
    @Column(name="updated_at", nullable=false)
    private OffsetDateTime updatedAt;


}
