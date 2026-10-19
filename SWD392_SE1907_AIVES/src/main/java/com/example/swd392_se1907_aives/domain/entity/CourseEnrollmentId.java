package com.example.swd392_se1907_aives.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class CourseEnrollmentId implements Serializable {
    @Column(name="course_id", nullable=false)
    private UUID courseId;

    @Column(name="student_id", nullable=false)
    private UUID studentId;

}
