package com.example.swd392_se1907_aives.modules.examination.internal.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ExamMonitorDtos {
    private ExamMonitorDtos() {}

    public record AttemptView(
            UUID id,
            int attemptNo,
            String status,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            OffsetDateTime lastActivityAt,
            int completedQuestions,
            int questionCount,
            int timedOutQuestions,
            int followupsUsed,
            Integer currentSequence,
            String phase,
            OffsetDateTime deadline) {}

    public record CandidateView(
            UUID id,
            String fullName,
            String email,
            OffsetDateTime scheduledStart,
            OffsetDateTime scheduledEnd,
            String status,
            List<AttemptView> attempts) {}

    public record MonitorView(
            UUID examId,
            String title,
            String examStatus,
            String courseCode,
            String courseName,
            OffsetDateTime serverNow,
            List<CandidateView> candidates) {}

    public record TurnView(
            UUID id,
            int sequenceNo,
            int turnNo,
            String speaker,
            String kind,
            int followupNumber,
            String text,
            String speechStatus,
            boolean hasAudio,
            OffsetDateTime createdAt,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            OffsetDateTime deadline) {}

    public record TranscriptView(
            UUID attemptId,
            String studentName,
            String email,
            int attemptNo,
            String status,
            OffsetDateTime startedAt,
            OffsetDateTime endedAt,
            OffsetDateTime serverNow,
            List<TurnView> turns) {}
}
