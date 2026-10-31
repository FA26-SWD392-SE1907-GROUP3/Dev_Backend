package com.example.swd392_se1907_aives.modules.examination.internal.service.impl;

import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamAttemptRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamCandidateRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamQuestionPoolRepository;
import com.example.swd392_se1907_aives.modules.examination.internal.repository.ExamRepository;
import com.example.swd392_se1907_aives.modules.teaching.api.TeachingUsagePolicy;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeachingUsagePolicyServiceImpl implements TeachingUsagePolicy {
    private final ExamRepository exams;
    private final ExamCandidateRepository candidates;
    private final ExamAttemptRepository attempts;
    private final ExamQuestionPoolRepository pool;

    @Override
    public boolean hasOpenExams(UUID courseId) {
        return exams.existsByCourseIdAndStatusIn(courseId, List.of("ACTIVE", "SCHEDULED"));
    }

    @Override
    public boolean hasStudentInProgress(UUID courseId, UUID studentId) {
        return candidates.findByStudentId(studentId).stream()
                .anyMatch(
                        candidate ->
                                courseId.equals(candidate.getCourseId())
                                        && attempts.existsByCandidateIdAndStatus(
                                                candidate.getId(), "IN_PROGRESS"));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void prepareQuestionChange(UUID questionId) {
        var links = pool.findByIdQuestionId(questionId);
        if (links.stream()
                .anyMatch(
                        link ->
                                exams.findById(link.getId().getExamId())
                                        .map(
                                                exam ->
                                                        Set.of("ACTIVE", "SCHEDULED")
                                                                .contains(exam.getStatus()))
                                        .orElse(false)))
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Câu hỏi đang dùng trong kỳ thi đã mở hoặc đã lên lịch. Chỉ chỉnh sửa/loại bỏ"
                            + " sau khi kỳ thi kết thúc.");
        pool.deleteAll(
                links.stream()
                        .filter(
                                link ->
                                        exams.findById(link.getId().getExamId())
                                                .map(exam -> "DRAFT".equals(exam.getStatus()))
                                                .orElse(false))
                        .toList());
    }

    @Override
    public boolean hasRubricInUse(UUID rubricId) {
        return exams.existsByRubricId(rubricId);
    }
}
