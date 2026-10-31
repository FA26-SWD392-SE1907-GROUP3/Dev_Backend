package com.example.swd392_se1907_aives.modules.examination.internal.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AttemptGradingDtos {
    private AttemptGradingDtos() {}

    public record CriterionScore(
            int sortOrder, String name, BigDecimal score, BigDecimal maxPoints, String feedback) {}

    public record GradingResult(
            BigDecimal totalScore,
            BigDecimal maxScore,
            String feedback,
            List<CriterionScore> criteria) {}

    public record GradingView(
            UUID attemptId,
            String status,
            String error,
            OffsetDateTime gradedAt,
            GradingResult result) {}
}
