package com.example.swd392_se1907_aives.modules.teaching.internal.service;

import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionDtos.QuestionRequest;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionDtos.QuestionView;

import java.util.List;
import java.util.UUID;

public interface QuestionService {
    QuestionView createQuestion(QuestionRequest request);

    List<QuestionView> list(UUID courseId);

    QuestionView review(UUID id, boolean approve, String comment);

    QuestionView updateQuestion(UUID id, QuestionRequest request);

    QuestionView archiveQuestion(UUID id);
}
