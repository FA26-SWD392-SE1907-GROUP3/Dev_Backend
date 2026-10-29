package com.example.swd392_se1907_aives.modules.examination.internal.service.impl;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.Exam;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamAttempt;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamCandidate;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.StudentExamDtos.AttemptView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.StudentExamDtos.CandidateView;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamAttemptRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamCandidateRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.service.StudentExamService;
import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentExamServiceImpl implements StudentExamService {
    private final ExamCandidateRepository candidates;
    private final ExamAttemptRepository attempts;
    private final CourseAccessService access;
    private final ExamRepository exams;

    @Override
    public List<CandidateView> myExams() {
        var schedules = candidates.findByStudentId(access.current().getId());
        Map<UUID, Exam> byExam = new HashMap<>();
        exams.findAllById(schedules.stream().map(ExamCandidate::getExamId).distinct().toList())
                .forEach(exam -> byExam.put(exam.getId(), exam));
        Map<UUID, ExamAttempt> latest = new HashMap<>();
        if (!schedules.isEmpty())
            attempts.findByCandidateIdInOrderByAttemptNoDesc(
                            schedules.stream().map(ExamCandidate::getId).toList())
                    .forEach(attempt -> latest.putIfAbsent(attempt.getCandidateId(), attempt));
        return schedules.stream()
                .map(
                        c ->
                                new CandidateView(
                                        c.getId(),
                                        c.getExamId(),
                                        byExam.containsKey(c.getExamId())
                                                ? byExam.get(c.getExamId()).getTitle()
                                                : "Kỳ thi không tồn tại",
                                        c.getCourseId(),
                                        c.getScheduledStart(),
                                        c.getScheduledEnd(),
                                        c.getStatus(),
                                        action(c, latest.get(c.getId())),
                                        latest.containsKey(c.getId())
                                                ? latest.get(c.getId()).getId()
                                                : null))
                .sorted(Comparator.comparing(CandidateView::scheduledStart))
                .toList();
    }

    @Override
    public List<AttemptView> myAttempts(UUID candidateId) {
        ExamCandidate c =
                candidates
                        .findById(candidateId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Candidate not found"));
        if (!c.getStudentId().equals(access.current().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your exam");
        }
        var history = attempts.findByCandidateIdOrderByAttemptNoAsc(candidateId);
        var latest = history.isEmpty() ? null : history.getLast();
        if (!"VIEW".equals(action(c, latest)))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Chỉ xem lịch sử sau khi thi xong hoặc lịch thi đã bị hủy. Lượt đang thi cần"
                            + " được tiếp tục.");
        return history.stream()
                .map(
                        a ->
                                new AttemptView(
                                        a.getId(),
                                        a.getCandidateId(),
                                        a.getExamId(),
                                        a.getAttemptNo(),
                                        a.getStatus(),
                                        a.getStartedAt(),
                                        a.getEndedAt()))
                .toList();
    }

    private String action(ExamCandidate candidate, ExamAttempt attempt) {
        if (Set.of("COMPLETED", "CANCELLED", "ABSENT").contains(candidate.getStatus())
                || attempt != null && Set.of("COMPLETED", "ABORTED").contains(attempt.getStatus()))
            return "VIEW";
        if (attempt != null && "IN_PROGRESS".equals(attempt.getStatus())) return "RESUME";
        if (attempt == null && Set.of("REGISTERED", "READY").contains(candidate.getStatus()))
            return "START";
        return "UNAVAILABLE";
    }
}
