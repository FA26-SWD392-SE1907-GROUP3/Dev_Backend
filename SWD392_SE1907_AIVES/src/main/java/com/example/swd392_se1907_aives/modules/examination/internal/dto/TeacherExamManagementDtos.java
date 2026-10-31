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
            String status) {}

    public record Schedule(
            @NotNull OffsetDateTime scheduledStart, @NotNull OffsetDateTime scheduledEnd) {}
}
