package com.example.swd392_se1907_aives.modules.examination.internal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class ExamDtos {
    private ExamDtos() {}

    public record ExamCreate(
            @NotNull UUID courseId,
            UUID rubricId,
            @NotBlank @Size(max = 200) String title,
            @Min(1) @Max(100) int mainQuestionCount,
            @Min(0) @Max(10) Integer maxFollowupsPerQuestion,
            @Min(15) @Max(600) Integer answerTimeLimitSeconds,
            @Min(0) @Max(100) Integer maxFollowupsPerStudent,
            @Pattern(regexp = "RANDOM|ADAPTIVE") String selectionMode) {}

    public record ExamView(
            UUID id,
            UUID courseId,
            UUID rubricId,
            String title,
            String status,
            String selectionMode,
            int mainQuestionCount,
            int maxFollowupsPerQuestion,
            int answerTimeLimitSeconds,
            int maxFollowupsPerStudent) {}

    public record QuestionId(@NotNull UUID questionId) {}

    public record CandidateCreate(
            @NotBlank @Email String studentEmail,
            @NotNull OffsetDateTime scheduledStart,
            @NotNull OffsetDateTime scheduledEnd,
            boolean enrollIfNeeded) {}
}
