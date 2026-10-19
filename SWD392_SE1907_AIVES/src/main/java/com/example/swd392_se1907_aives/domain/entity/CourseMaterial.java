package com.example.swd392_se1907_aives.domain.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="course_materials")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseMaterial {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="course_id", nullable=false)
    private UUID courseId;
    @Column(name="uploaded_by", nullable=false)
    private UUID uploadedBy;
    @Column(name="original_filename", nullable=false, length=255)
    private String originalFilename;
    @Column(name="object_key", nullable=false)
    private String objectKey;
    @Column(name="mime_type", nullable=false, length=100)
    private String mimeType;
    @Column(name="file_sha256", nullable=true, length=64)
    private String fileSha256;
    @Column(name="extraction_status", nullable=false, length=20)
    private String extractionStatus;
    @Column(name="extracted_text_object_key", nullable=true)
    private String extractedTextObjectKey;
    @Column(name="uploaded_at", nullable=false)
    private OffsetDateTime uploadedAt;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="course_id", insertable=false, updatable=false)
    private Course courseIdRef;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="uploaded_by", insertable=false, updatable=false)
    private User uploadedByRef;
}
