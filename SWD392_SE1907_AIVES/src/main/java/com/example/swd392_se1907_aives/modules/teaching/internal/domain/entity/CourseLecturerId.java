package com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity;



import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class CourseLecturerId implements Serializable {
    @Column(name="course_id", nullable=false)
    private UUID courseId;

    @Column(name="lecturer_id", nullable=false)
    private UUID lecturerId;

}
