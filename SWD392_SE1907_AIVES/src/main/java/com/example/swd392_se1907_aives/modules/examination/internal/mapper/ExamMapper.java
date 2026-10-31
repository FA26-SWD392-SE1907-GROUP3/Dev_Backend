package com.example.swd392_se1907_aives.modules.examination.internal.mapper;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.Exam;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.ExamCreate;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.ExamView;

public final class ExamMapper {
    private ExamMapper() {}

    public static void applySettings(Exam exam, ExamCreate request) {
        int followupsPerQuestion =
                request.maxFollowupsPerQuestion() == null ? 2 : request.maxFollowupsPerQuestion();
        exam.setRubricId(request.rubricId());
        exam.setTitle(request.title());
        exam.setMainQuestionCount(request.mainQuestionCount());
        exam.setMaxFollowupsPerQuestion(followupsPerQuestion);
        exam.setMaxFollowupsPerStudent(
                request.maxFollowupsPerStudent() == null
                        ? Math.min(100, followupsPerQuestion * request.mainQuestionCount())
                        : request.maxFollowupsPerStudent());
        exam.setSelectionMode(request.selectionMode() == null ? "RANDOM" : request.selectionMode());
        exam.setAnswerTimeLimitSeconds(
                request.answerTimeLimitSeconds() == null ? 120 : request.answerTimeLimitSeconds());
    }

    public static ExamView view(Exam e) {
        return new ExamView(
                e.getId(),
                e.getCourseId(),
                e.getRubricId(),
                e.getTitle(),
                e.getStatus(),
                e.getSelectionMode(),
                e.getMainQuestionCount(),
                e.getMaxFollowupsPerQuestion(),
                e.getAnswerTimeLimitSeconds(),
                e.followupBudget());
    }
}
