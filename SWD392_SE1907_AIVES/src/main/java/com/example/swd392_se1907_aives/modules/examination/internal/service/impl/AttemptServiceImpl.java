package com.example.swd392_se1907_aives.modules.examination.internal.service.impl;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.AttemptQuestion;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.Exam;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamAttempt;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamCandidate;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamQuestionPool;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.AttemptDtos.AnswerRequest;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.AttemptDtos.AssignedQuestion;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.AttemptDtos.AttemptView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.InterviewDtos.State;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.AttemptQuestionRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamAttemptRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamCandidateRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamQuestionPoolRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.InterviewTurnRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.service.AttemptService;
import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewService;
import com.example.swd392_se1907_aives.modules.examination.internal.service.QuestionSetSelector;
import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;
import com.example.swd392_se1907_aives.modules.teaching.api.CriterionInfo;
import com.example.swd392_se1907_aives.modules.teaching.api.QuestionCatalog;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class AttemptServiceImpl implements AttemptService {
    private final ExamCandidateRepository candidates;
    private final ExamRepository exams;
    private final ExamQuestionPoolRepository pool;
    private final QuestionCatalog questions;
    private final ExamAttemptRepository attempts;
    private final AttemptQuestionRepository attemptQuestions;
    private final InterviewTurnRepository turns;
    private final CourseAccessService access;
    private final com.fasterxml.jackson.databind.ObjectMapper json;
    private final QuestionSetSelector selector;
    private final InterviewService interview;

    @Override
    @Transactional
    public AttemptView start(UUID candidateId) {
        UUID examId =
                candidates
                        .findExamIdById(candidateId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Candidate not found"));
        Exam e =
                exams.findLockedById(examId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Exam not found"));
        ExamCandidate c =
                candidates
                        .findLockedById(candidateId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Candidate not found"));
        if (!c.getStudentId().equals(access.current().getId()))
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Lịch thi này được phân cho sinh viên khác. Hãy mở kỳ thi trong danh sách lịch"
                            + " thi của bạn.");
        access.requireEnrolled(c.getCourseId());
        var now = OffsetDateTime.now();
        if (!"ACTIVE".equals(e.getStatus())
                || now.isBefore(c.getScheduledStart())
                || now.isAfter(c.getScheduledEnd()))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Kỳ thi chưa được kích hoạt hoặc hiện không nằm trong khung giờ thi của bạn."
                            + " Hãy kiểm tra lịch thi và liên hệ giảng viên.");
        if ("CANCELLED".equals(c.getStatus()))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Lịch thi đã bị giảng viên hủy. Bạn chỉ có thể xem lịch sử lượt thi.");
        var existing = attempts.findByCandidateIdOrderByAttemptNoAsc(candidateId);
        if (!existing.isEmpty()) {
            var previous = existing.getLast();
            if ("IN_PROGRESS".equals(previous.getStatus()) && "IN_PROGRESS".equals(c.getStatus()))
                return new AttemptView(
                        previous.getId(),
                        candidateId,
                        previous.getStatus(),
                        attemptQuestions.findByAttemptIdOrderBySequenceNo(previous.getId()).size());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Lượt thi đã kết thúc. Bạn chỉ có thể xem lịch sử lượt thi.");
        }
        if (!Set.of("REGISTERED", "READY").contains(c.getStatus()))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Lịch thi không còn cho phép bắt đầu. Hãy làm mới danh sách lịch thi.");
        List<ExamQuestionPool> eligible =
                pool.findByIdExamId(e.getId()).stream()
                        .filter(
                                p ->
                                        questions
                                                .findById(p.getId().getQuestionId())
                                                .map(q -> "APPROVED".equals(q.getStatus()))
                                                .orElse(false))
                        .toList();
        if (eligible.size() < e.getMainQuestionCount())
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Exam question pool is insufficient");
        var recent = attempts.findTop20ByExamIdOrderByStartedAtDesc(e.getId());
        Set<UUID> previous = new HashSet<>();
        Map<UUID, Integer> usage = new HashMap<>();
        for (int i = 0; i < recent.size(); i++)
            for (var item :
                    attemptQuestions.findByAttemptIdOrderBySequenceNo(recent.get(i).getId())) {
                usage.merge(item.getQuestionId(), 1, Integer::sum);
                if (i == 0) previous.add(item.getQuestionId());
            }
        var selected =
                selector.select(
                        eligible.stream()
                                .map(
                                        item ->
                                                questions
                                                        .findById(item.getId().getQuestionId())
                                                        .orElseThrow())
                                .toList(),
                        e.getMainQuestionCount(),
                        e.getSelectionMode(),
                        previous,
                        usage);
        ExamAttempt attempt = new ExamAttempt();
        attempt.setCandidateId(candidateId);
        attempt.setExamId(e.getId());
        attempt.setAttemptNo(1);
        attempt.setStatus("IN_PROGRESS");
        attempt.setStartedAt(now);
        attempt.setLastActivityAt(now);
        attempt.setCreatedAt(now);
        if (e.getRubricId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Kỳ thi chưa có rubric chấm toàn bài. Liên hệ giảng viên trước khi bắt đầu.");
        }
        {
            var rubric =
                    questions
                            .findRubricById(e.getRubricId())
                            .orElseThrow(
                                    () ->
                                            new ResponseStatusException(
                                                    HttpStatus.CONFLICT,
                                                    "Không tìm thấy rubric chấm bài."));
            if (!e.getCourseId().equals(rubric.getCourseId())
                    || !"PUBLISHED".equals(rubric.getStatus())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Rubric chấm bài không còn hợp lệ.");
            }
            attempt.setRubricSnapshot(
                    json.valueToTree(
                                    Map.of(
                                            "id",
                                            rubric.getId(),
                                            "name",
                                            rubric.getName(),
                                            "maxScore",
                                            rubric.getMaxScore(),
                                            "criteria",
                                            questions.criteria(rubric.getId()).stream()
                                                    .sorted(
                                                            Comparator.comparing(
                                                                    CriterionInfo::getSortOrder))
                                                    .map(
                                                            criterion ->
                                                                    Map.of(
                                                                            "sortOrder",
                                                                            criterion
                                                                                    .getSortOrder(),
                                                                            "name",
                                                                            criterion
                                                                                    .getCriterionName(),
                                                                            "description",
                                                                            Objects.toString(
                                                                                    criterion
                                                                                            .getDescription(),
                                                                                    ""),
                                                                            "maxPoints",
                                                                            criterion
                                                                                    .getMaxPoints()))
                                                    .toList()))
                            .toString());
        }
        attempt = attempts.saveAndFlush(attempt);
        for (int i = 0; i < e.getMainQuestionCount(); i++) {
            var q = selected.get(i);
            AttemptQuestion aq = new AttemptQuestion();
            aq.setAttemptId(attempt.getId());
            aq.setExamId(e.getId());
            aq.setQuestionId(q.getId());
            aq.setSequenceNo(i + 1);
            aq.setQuestionTextSnapshot(q.getContent());
            aq.setReferenceAnswerSnapshot(q.getReferenceAnswer());
            aq.setSelectionMode(e.getSelectionMode());
            aq.setStatus("PENDING");
            aq.setCreatedAt(now);
            attemptQuestions.saveAndFlush(aq);
        }
        c.setStatus("IN_PROGRESS");
        candidates.save(c);
        return new AttemptView(
                attempt.getId(), candidateId, attempt.getStatus(), e.getMainQuestionCount());
    }

    @Override
    public List<AssignedQuestion> assigned(UUID id) {
        interview.state(id);
        var a = own(id);
        return attemptQuestions.findByAttemptIdOrderBySequenceNo(a.getId()).stream()
                .filter(q -> !"PENDING".equals(q.getStatus()))
                .map(
                        q ->
                                new AssignedQuestion(
                                        q.getId(),
                                        q.getSequenceNo(),
                                        q.getQuestionTextSnapshot(),
                                        q.getStatus()))
                .toList();
    }

    @Override
    public State answer(UUID assignedQuestionId, AnswerRequest request) {
        var aq =
                attemptQuestions
                        .findById(assignedQuestionId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Question not assigned"));
        var state = interview.state(aq.getAttemptId());
        if (state.prompt() == null || !assignedQuestionId.equals(state.prompt().questionId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Question is not current");
        return interview.textAnswer(aq.getAttemptId(), request.promptId(), request.text());
    }

    private ExamAttempt own(UUID id) {
        var a =
                attempts.findById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Attempt not found"));
        var c = candidates.findById(a.getCandidateId()).orElseThrow();
        if (!c.getStudentId().equals(access.current().getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your attempt");
        return a;
    }
}
