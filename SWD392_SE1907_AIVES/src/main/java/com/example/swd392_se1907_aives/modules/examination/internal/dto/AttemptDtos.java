package com.example.swd392_se1907_aives.modules.examination.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AttemptDtos {
    private AttemptDtos() {}

    public record AttemptView(UUID attemptId, UUID candidateId, String status, int questionCount) {}

    public record AssignedQuestion(UUID id, int sequenceNo, String content, String status) {}

    public record AnswerRequest(@NotNull UUID promptId, @NotBlank @Size(max = 12000) String text) {}
}
