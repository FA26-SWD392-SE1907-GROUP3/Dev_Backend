package com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity;


import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.databind.JsonNode;
@Entity
@DynamicInsert
@Table(name="question_import_batches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuestionImportBatch {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="course_id", nullable=false)
    private UUID courseId;
    @Column(name="imported_by", nullable=false)
    private UUID importedBy;
    @Column(name="original_filename", nullable=false, length=255)
    private String originalFilename;
    @Column(name="status", nullable=false, length=20)
    private String status;
    @Column(name="total_rows", nullable=false)
    private Integer totalRows;
    @Column(name="imported_rows", nullable=false)
    private Integer importedRows;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="error_details", nullable=false)
    private JsonNode errorDetails;
    @Column(name="imported_at", nullable=false)
    private OffsetDateTime importedAt;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="course_id", insertable=false, updatable=false)
    private Course courseIdRef;

}
