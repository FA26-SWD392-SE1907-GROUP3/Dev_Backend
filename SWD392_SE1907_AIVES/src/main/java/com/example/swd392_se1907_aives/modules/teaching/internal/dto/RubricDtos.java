package com.example.swd392_se1907_aives.modules.teaching.internal.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public final class RubricDtos {
    private RubricDtos() {}

    public record RubricRequest(
            @NotNull UUID courseId,
            @NotBlank @Size(max = 180) String name,
            String description,
            @NotNull @DecimalMin("0.01") @Digits(integer = 4, fraction = 2) BigDecimal maxScore) {}

    public record RubricView(
            UUID id,
            UUID courseId,
            String name,
            BigDecimal maxScore,
            String status,
            String description) {}
}
