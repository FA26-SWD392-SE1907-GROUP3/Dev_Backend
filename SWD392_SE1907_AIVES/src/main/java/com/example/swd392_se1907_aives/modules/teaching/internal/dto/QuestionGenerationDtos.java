package com.example.swd392_se1907_aives.modules.teaching.internal.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class QuestionGenerationDtos {
    private QuestionGenerationDtos() {}

    public record GenerateRequest(
            @NotEmpty @Size(max = 5) List<@NotNull UUID> materialIds,
            @Min(1) @Max(20) int desiredCount,
            @NotNull @Pattern(regexp = "EASY|MEDIUM|HARD") String difficulty,
            @Size(max = 2000) String instruction) {}

    public record AiStatus(boolean configured, String model) {}

    public record JobView(
            UUID id,
            UUID courseId,
            String status,
            int generatedCount,
            int desiredCount,
            String errorMessage,
            OffsetDateTime createdAt) {}

    public record GeneratedQuestionView(
            UUID id, String content, String referenceAnswer, String difficulty, String status) {}
}
