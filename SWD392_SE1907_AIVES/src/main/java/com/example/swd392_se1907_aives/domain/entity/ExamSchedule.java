package com.example.swd392_se1907_aives.domain.entity;

import com.example.swd392_se1907_aives.domain.enums.ScheduleStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ExamSchedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ScheduleID")
    private Integer scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ExamID", nullable = false)
    private ExamSession examSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StudentID", nullable = false)
    private User student;

    @Column(name = "AllocatedStartTime")
    private LocalDateTime allocatedStartTime;

    @Column(name = "AllocatedEndTime")
    private LocalDateTime allocatedEndTime;

    @Column(name = "ActualStartTime")
    private LocalDateTime actualStartTime;

    @Column(name = "ActualEndTime")
    private LocalDateTime actualEndTime;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "Status", length = 50)
    private ScheduleStatus status;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = ScheduleStatus.NOT_STARTED;
        }
    }
}