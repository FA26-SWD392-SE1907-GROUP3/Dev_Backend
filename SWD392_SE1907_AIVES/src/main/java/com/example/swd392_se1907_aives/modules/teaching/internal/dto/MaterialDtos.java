package com.example.swd392_se1907_aives.modules.teaching.internal.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class MaterialDtos {
    private MaterialDtos() {}

    public record MaterialView(
            UUID id,
            UUID courseId,
            String originalFilename,
            String mimeType,
            String extractionStatus,
            OffsetDateTime uploadedAt) {}

    public record ChunkView(UUID id, int chunkIndex, String content, Integer pageNumber) {}

    public record Rename(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 255)
                    String name) {}
}
