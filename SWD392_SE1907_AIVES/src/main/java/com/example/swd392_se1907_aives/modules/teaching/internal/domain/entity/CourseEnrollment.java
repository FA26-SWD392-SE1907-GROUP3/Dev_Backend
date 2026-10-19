package com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="course_enrollments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseEnrollment {
    @EmbeddedId
    private CourseEnrollmentId id;
    @Column(name="enrollment_status", nullable=false, length=20)
    private String enrollmentStatus;
    @Column(name="enrolled_at", nullable=false)
    private OffsetDateTime enrolledAt;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="course_id", insertable=false, updatable=false)
    private Course courseIdRef;

}
