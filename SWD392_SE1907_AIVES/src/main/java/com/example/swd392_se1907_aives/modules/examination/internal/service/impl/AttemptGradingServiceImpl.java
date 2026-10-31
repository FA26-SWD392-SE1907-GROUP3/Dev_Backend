package com.example.swd392_se1907_aives.modules.examination.internal.service.impl;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamAttempt;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.AttemptGradingDtos.GradingResult;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.AttemptGradingDtos.GradingView;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.AttemptQuestionRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamAttemptRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamCandidateRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.InterviewTurnRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.service.AttemptGradeValidator;
import com.example.swd392_se1907_aives.modules.examination.internal.service.AttemptGradingAiClient;
import com.example.swd392_se1907_aives.modules.examination.internal.service.AttemptGradingService;
import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttemptGradingServiceImpl implements AttemptGradingService {
    private final ExamAttemptRepository attempts;
    private final ExamCandidateRepository candidates;
    private final AttemptQuestionRepository questions;
    private final InterviewTurnRepository turns;
    private final CourseAccessService access;
    private final AttemptGradingAiClient ai;
    private final AttemptGradeValidator validator;
    private final ObjectMapper json;
    private final PlatformTransactionManager transactions;

    private record Work(UUID id, String rubric, String evidence) {}

    @Override
    public GradingView get(UUID attemptId) {
        return view(authorized(attemptId));
    }

    @Override
    public GradingView retry(UUID attemptId) {
        authorized(attemptId);
        return new TransactionTemplate(transactions)
                .execute(
                        status -> {
                            var attempt = attempts.findLockedById(attemptId).orElseThrow();
                            if (!"COMPLETED".equals(attempt.getStatus()))
                                throw conflict("Chỉ chấm sau khi toàn bộ lượt thi đã kết thúc.");
                            if (attempt.getRubricSnapshot() == null)
                                throw conflict("Lượt thi này chưa lưu rubric chấm toàn bài.");
                            boolean stalled =
                                    "RUNNING".equals(attempt.getGradingStatus())
                                            && attempt.getGradingRequestedAt() != null
                                            && attempt.getGradingRequestedAt()
                                                    .isBefore(OffsetDateTime.now().minusMinutes(3));
                            if (!"FAILED".equals(attempt.getGradingStatus()) && !stalled)
                                throw conflict("Bài đang chờ chấm, đang chấm hoặc đã có kết quả.");
                            attempt.setGradingStatus("PENDING");
                            attempt.setGradingError(null);
                            attempts.save(attempt);
                            return view(attempt);
                        });
    }

    @Override
    public void processPending() {
        for (var pending :
                attempts.findTop10ByStatusAndGradingStatusOrderByEndedAtAsc(
                        "COMPLETED", "PENDING")) {
            Work work;
            try {
                work = claim(pending.getId());
            } catch (RuntimeException error) {
                fail(pending.getId(), "Không chuẩn bị được dữ liệu chấm toàn bài.");
                continue;
            }
            if (work == null) continue;
            try {
                var result =
                        validator.validate(json.readTree(work.rubric()), ai.grade(work.evidence()));
                new TransactionTemplate(transactions)
                        .executeWithoutResult(
                                status -> {
                                    var attempt = attempts.findLockedById(work.id()).orElseThrow();
                                    attempt.setGradingResult(json.valueToTree(result).toString());
                                    attempt.setGradingStatus("COMPLETED");
                                    attempt.setGradingError(null);
                                    attempt.setGradedAt(OffsetDateTime.now());
                                    attempts.save(attempt);
                                });
            } catch (Exception error) {
                log.warn(
                        "Whole-attempt grading failed for {} ({})",
                        work.id(),
                        error.getClass().getSimpleName());
                fail(
                        work.id(),
                        error instanceof IllegalArgumentException
                                ? "Kết quả AI không khớp rubric. Hãy thử chấm lại."
                                : "Chưa chấm được toàn bài. Kiểm tra cấu hình AI và thử lại.");
            }
        }
    }

    private Work claim(UUID id) {
        return new TransactionTemplate(transactions)
                .execute(
                        status -> {
                            var attempt = attempts.findLockedById(id).orElseThrow();
                            if (!"COMPLETED".equals(attempt.getStatus())
                                    || !"PENDING".equals(attempt.getGradingStatus())) return null;
                            if (attempt.getRubricSnapshot() == null) {
                                attempt.setGradingStatus("FAILED");
                                attempt.setGradingError(
                                        "Lượt thi cũ chưa có rubric chấm toàn bài.");
                                attempts.save(attempt);
                                return null;
                            }
                            List<Map<String, Object>> evidence = new ArrayList<>();
                            for (var question : questions.findByAttemptIdOrderBySequenceNo(id)) {
                                var history =
                                        turns
                                                .findByAttemptQuestionIdOrderByTurnNo(
                                                        question.getId())
                                                .stream()
                                                .filter(turn -> turn.getTextContent() != null)
                                                .map(
                                                        turn ->
                                                                Map.of(
                                                                        "speaker",
                                                                        turn.getSpeaker(),
                                                                        "kind",
                                                                        turn.getKind(),
                                                                        "text",
                                                                        turn.getTextContent(),
                                                                        "speechStatus",
                                                                        turn.getSpeechStatus()))
                                                .toList();
                                evidence.add(
                                        Map.of(
                                                "sequenceNo",
                                                question.getSequenceNo(),
                                                "question",
                                                question.getQuestionTextSnapshot(),
                                                "referenceAnswer",
                                                Objects.toString(
                                                        question.getReferenceAnswerSnapshot(), ""),
                                                "status",
                                                question.getStatus(),
                                                "conversation",
                                                history));
                            }
                            String payload;
                            try {
                                payload =
                                        json.writeValueAsString(
                                                Map.of(
                                                        "rubric",
                                                        json.readTree(attempt.getRubricSnapshot()),
                                                        "wholeAttempt",
                                                        evidence));
                            } catch (Exception error) {
                                throw new IllegalStateException("Invalid grading snapshot");
                            }
                            attempt.setGradingStatus("RUNNING");
                            attempt.setGradingRequestedAt(OffsetDateTime.now());
                            attempt.setGradingError(null);
                            attempts.save(attempt);
                            return new Work(id, attempt.getRubricSnapshot(), payload);
                        });
    }

    private void fail(UUID id, String message) {
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        status -> {
                            var attempt = attempts.findLockedById(id).orElseThrow();
                            if ("COMPLETED".equals(attempt.getGradingStatus())) return;
                            attempt.setGradingStatus("FAILED");
                            attempt.setGradingError(message);
                            attempts.save(attempt);
                        });
    }

    private ExamAttempt authorized(UUID id) {
        var attempt =
                attempts.findById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Không tìm thấy lượt thi."));
        var candidate = candidates.findById(attempt.getCandidateId()).orElseThrow();
        var actor = access.current();
        if ("STUDENT".equals(actor.getRole())) {
            if (!actor.getId().equals(candidate.getStudentId()))
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Không phải bài thi của bạn.");
        } else access.requireTeacher(candidate.getCourseId());
        return attempt;
    }

    private GradingView view(ExamAttempt attempt) {
        String status = attempt.getGradingStatus();
        if (!"COMPLETED".equals(attempt.getStatus())) status = "NOT_STARTED";
        else if (status == null) status = "UNAVAILABLE";
        else if ("RUNNING".equals(status)
                && attempt.getGradingRequestedAt() != null
                && attempt.getGradingRequestedAt().isBefore(OffsetDateTime.now().minusMinutes(3))) {
            status = "FAILED";
        }
        GradingResult result = null;
        try {
            if ("COMPLETED".equals(status))
                result = json.readValue(attempt.getGradingResult(), GradingResult.class);
        } catch (Exception error) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Không đọc được kết quả chấm bài.");
        }
        return new GradingView(
                attempt.getId(), status, attempt.getGradingError(), attempt.getGradedAt(), result);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
