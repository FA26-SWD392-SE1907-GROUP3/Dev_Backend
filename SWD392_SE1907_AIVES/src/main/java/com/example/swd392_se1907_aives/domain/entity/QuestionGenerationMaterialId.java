package com.example.swd392_se1907_aives.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode
public class QuestionGenerationMaterialId implements Serializable {
    @Column(name="job_id", nullable=false)
    private UUID jobId;

    @Column(name="material_id", nullable=false)
    private UUID materialId;

}
