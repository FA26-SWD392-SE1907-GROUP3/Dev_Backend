package com.example.swd392_se1907_aives.modules.teaching.internal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public final class RubricCriterionDtos {
    private RubricCriterionDtos() {}

    public record CriterionRequest(
            @NotBlank @Size(max = 180) String criterionName,
            String description,
            @NotNull @DecimalMin("0.01") @Digits(integer = 4, fraction = 2) BigDecimal maxPoints,
            @Min(1) int sortOrder) {}

    public record CriterionView(
            UUID id,
            UUID rubricId,
            String criterionName,
            String description,
            BigDecimal maxPoints,
            int sortOrder) {}
}
