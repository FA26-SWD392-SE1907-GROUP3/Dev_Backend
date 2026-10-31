package com.example.swd392_se1907_aives.modules.teaching.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public final class QuestionDtos {
    private QuestionDtos() {}

    public record QuestionRequest(
            @NotNull UUID courseId,
            @NotBlank String content,
            String referenceAnswer,
            @NotNull @Pattern(regexp = "EASY|MEDIUM|HARD") String difficulty) {}

    public record QuestionView(
            UUID id, UUID courseId, String content, String difficulty, String status) {}
}
