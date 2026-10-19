package com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="rubric_criteria")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RubricCriterion {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="rubric_id", nullable=false)
    private UUID rubricId;
    @Column(name="criterion_name", nullable=false, length=180)
    private String criterionName;
    @Column(name="description", nullable=true)
    private String description;
    @Column(name="max_points", nullable=false, precision=6, scale=2)
    private BigDecimal maxPoints;
    @Column(name="sort_order", nullable=false)
    private Integer sortOrder;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="rubric_id", insertable=false, updatable=false)
    private Rubric rubricIdRef;
}
