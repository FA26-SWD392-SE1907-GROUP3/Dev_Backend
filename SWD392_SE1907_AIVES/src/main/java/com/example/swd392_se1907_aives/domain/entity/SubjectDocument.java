package com.example.swd392_se1907_aives.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "SubjectDocuments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubjectDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DocumentID")
    private Integer documentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SubjectID", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UploadedBy", nullable = false)
    private User uploadedBy;

    @Column(name = "Title", nullable = false, length = 200)
    private String title;

    @Column(name = "FilePath", nullable = false, length = 500)
    private String filePath;

    @Column(name = "FileType", length = 20)
    private String fileType;

    @Column(name = "Status", length = 30)
    private String status;

    @Column(name = "ExtractedText", columnDefinition = "NVARCHAR(MAX)")
    private String extractedText;

    @Column(name = "UploadedAt")
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PROCESSING";
        }
    }
}
