package com.example.swd392_se1907_aives.modules.examination.internal.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class StudentExamDtos {
    private StudentExamDtos() {}

    public record CandidateView(
            UUID id,
            UUID examId,
            String examTitle,
            UUID courseId,
            OffsetDateTime scheduledStart,
            OffsetDateTime scheduledEnd,
            String status,
            String action,
            UUID attemptId) {}

    public record AttemptView(
            UUID id,
            UUID candidateId,
            UUID examId,
            Integer attemptNo,
            String status,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt) {}
}
