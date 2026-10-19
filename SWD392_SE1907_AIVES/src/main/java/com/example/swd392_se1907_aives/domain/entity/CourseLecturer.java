package com.example.swd392_se1907_aives.domain.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="course_lecturers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseLecturer {
    @EmbeddedId
    private CourseLecturerId id;
    @Column(name="assigned_at", nullable=false)
    private OffsetDateTime assignedAt;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="course_id", insertable=false, updatable=false)
    private Course courseIdRef;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="lecturer_id", insertable=false, updatable=false)
    private User lecturerIdRef;
}
