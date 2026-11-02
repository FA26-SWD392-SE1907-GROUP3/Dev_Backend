package com.example.swd392_se1907_aives.modules.examination.internal.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class TeacherExamManagementDtos {
    private TeacherExamManagementDtos() {}

    public record PoolView(UUID id, String content, String difficulty, String status) {}

    public record CandidateView(
            UUID id,
            UUID studentId,
            String fullName,
            String studentEmail,
            OffsetDateTime scheduledStart,
            OffsetDateTime scheduledEnd,
            String status,
            UUID latestAttemptId,
            boolean canRetake,
            boolean retakePending,
            String retakeReason) {}

    public record RetakeRequest(
            @NotNull UUID previousAttemptId,
            @NotNull OffsetDateTime scheduledStart,
            @NotNull OffsetDateTime scheduledEnd,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=1000) String reason) {}

    public record RetakeView(UUID id, UUID previousAttemptId, int nextAttemptNo, UUID grantedBy,
            String reason, OffsetDateTime scheduledStart, OffsetDateTime scheduledEnd,
            OffsetDateTime grantedAt, OffsetDateTime consumedAt, OffsetDateTime revokedAt) {}

    public record Schedule(
            @NotNull OffsetDateTime scheduledStart, @NotNull OffsetDateTime scheduledEnd) {}
}
