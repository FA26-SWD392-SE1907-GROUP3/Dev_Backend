package com.example.swd392_se1907_aives.modules.examination.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class InterviewDtos {
    private InterviewDtos() {}

    public record PromptRequest(@NotNull UUID promptId) {}

    public record AnswerRequest(@NotNull UUID promptId, @NotBlank @Size(max = 12000) String text) {}

    public record ContinueRequest(@NotNull UUID answerTurnId) {}

    public record Prompt(
            UUID id,
            UUID questionId,
            int sequenceNo,
            String kind,
            int followupNumber,
            String text,
            OffsetDateTime deadline) {}

    public record TurnView(
            UUID id,
            UUID questionId,
            int sequenceNo,
            String speaker,
            String kind,
            int followupNumber,
            String text,
            String speechStatus,
            boolean hasAudio) {}

    public record State(
            UUID attemptId,
            String status,
            int questionCount,
            int completedQuestions,
            int maxFollowups,
            int followupsUsed,
            int maxFollowupsPerQuestion,
            int answerTimeLimitSeconds,
            OffsetDateTime scheduledEnd,
            OffsetDateTime serverNow,
            Prompt prompt,
            boolean busy,
            boolean needsContinuation,
            UUID answerTurnId,
            String error,
            boolean aiConfigured,
            List<TurnView> turns) {}
}
