package com.example.swd392_se1907_aives.modules.examination.internal.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AttemptGradingDtos {
    private AttemptGradingDtos() {}

    public record CriterionScore(
            int sortOrder, String name, BigDecimal score, BigDecimal maxPoints, String feedback) {}

    public record GradingResult(
            BigDecimal totalScore,
            BigDecimal maxScore,
            String feedback,
            List<CriterionScore> criteria) {}

    public record PublishRequest(
            @NotNull @DecimalMin("0") @Digits(integer=4, fraction=2) BigDecimal score,
            @NotBlank @Size(max=4000) String feedback) {}

    public record PublishedGrade(
            BigDecimal score, BigDecimal maxScore, String feedback, OffsetDateTime publishedAt) {}

    public record GradingView(
            UUID attemptId,
            String status,
            String error,
            OffsetDateTime gradedAt,
            GradingResult result,
            BigDecimal maxScore,
            PublishedGrade published) {}
}
