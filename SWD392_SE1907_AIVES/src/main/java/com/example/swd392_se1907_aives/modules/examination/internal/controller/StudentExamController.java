package com.example.swd392_se1907_aives.modules.examination.internal.controller;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamCandidate;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamAttemptRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamCandidateRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamRepository;
import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.*;

/** Read-only endpoints for student exam dashboard. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentExamController {
    private final ExamCandidateRepository candidates;
    private final ExamAttemptRepository attempts;
    private final CourseAccessService access;
    private final ExamRepository exams;

    public record CandidateView(UUID id, UUID examId, String examTitle, UUID courseId,
                                OffsetDateTime scheduledStart, OffsetDateTime scheduledEnd, String status) {}
    public record AttemptView(UUID id, UUID candidateId, UUID examId,
                              Integer attemptNo, String status,
                              OffsetDateTime startedAt, OffsetDateTime endedAt) {}

    @GetMapping("/exams")
    public List<CandidateView> myExams() {
        Map<UUID,String> titles = new HashMap<>();
        return candidates.findByStudentId(access.current().getId()).stream()
            .map(c -> new CandidateView(c.getId(), c.getExamId(),
                titles.computeIfAbsent(c.getExamId(), id -> exams.findById(id).map(e -> e.getTitle()).orElse("Kỳ thi không tồn tại")), c.getCourseId(),
                c.getScheduledStart(), c.getScheduledEnd(), c.getStatus()))
            .sorted(Comparator.comparing(CandidateView::scheduledStart))
            .toList();
    }

    @GetMapping("/exams/{candidateId}/attempts")
    public List<AttemptView> myAttempts(@PathVariable UUID candidateId) {
        ExamCandidate c = candidates.findById(candidateId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidate not found"));
        if (!c.getStudentId().equals(access.current().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your exam");
        }
        return attempts.findByCandidateIdOrderByAttemptNoAsc(candidateId).stream()
            .map(a -> new AttemptView(a.getId(), a.getCandidateId(), a.getExamId(),
                a.getAttemptNo(), a.getStatus(), a.getStartedAt(), a.getEndedAt()))
            .toList();
    }
}
