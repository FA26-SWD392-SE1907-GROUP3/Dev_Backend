package com.example.swd392_se1907_aives.domain.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="material_chunks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MaterialChunk {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="material_id", nullable=false)
    private UUID materialId;
    @Column(name="chunk_index", nullable=false)
    private Integer chunkIndex;
    @Column(name="content", nullable=false, columnDefinition="text")
    private String content;
    @Column(name="token_count", nullable=true)
    private Integer tokenCount;
    @Column(name="page_number", nullable=true)
    private Integer pageNumber;
    @Column(name="external_vector_id", nullable=true, length=255)
    private String externalVectorId;
    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="material_id", insertable=false, updatable=false)
    private CourseMaterial materialIdRef;
}
