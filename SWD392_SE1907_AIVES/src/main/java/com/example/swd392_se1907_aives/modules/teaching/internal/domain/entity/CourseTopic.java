package com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="course_topics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseTopic {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="course_id", nullable=false)
    private UUID courseId;
    @Column(name="name", nullable=false, length=150)
    private String name;
    @Column(name="slug", nullable=false, length=160)
    private String slug;
    @Column(name="description", nullable=true)
    private String description;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="course_id", insertable=false, updatable=false)
    private Course courseIdRef;
}
