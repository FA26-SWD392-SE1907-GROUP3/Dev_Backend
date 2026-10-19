package com.example.swd392_se1907_aives.domain.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="courses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Course {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="code", nullable=false, length=30)
    private String code;
    @Column(name="name", nullable=false, length=200)
    private String name;
    @Column(name="description", nullable=true, columnDefinition="text")
    private String description;
    @Column(name="is_active", nullable=false)
    private Boolean isActive;
    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;
}
