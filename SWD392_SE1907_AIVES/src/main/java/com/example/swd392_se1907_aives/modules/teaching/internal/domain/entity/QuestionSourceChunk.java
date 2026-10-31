package com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="question_source_chunks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuestionSourceChunk {
    @EmbeddedId
    private QuestionSourceChunkId id;
    @Column(name="relevance_score", nullable=true, precision=5, scale=4)
    private BigDecimal relevanceScore;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="question_id", insertable=false, updatable=false)
    private Question questionIdRef;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="chunk_id", insertable=false, updatable=false)
    private MaterialChunk chunkIdRef;
}
