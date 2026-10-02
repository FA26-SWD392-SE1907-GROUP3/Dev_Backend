package com.example.swd392_se1907_aives.domain.entity;

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

    @Column(name = "Status", length = 50)
    private String status;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = "Not_Started";
        }
    }
}