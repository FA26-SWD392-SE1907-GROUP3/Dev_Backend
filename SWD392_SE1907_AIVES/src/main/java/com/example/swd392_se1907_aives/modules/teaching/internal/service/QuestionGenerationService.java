package com.example.swd392_se1907_aives.modules.teaching.internal.service;

import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Question;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionGenerationJob;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionGenerationDtos.AiStatus;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionGenerationDtos.GenerateRequest;

import java.util.List;
import java.util.UUID;

public interface QuestionGenerationService {
    List<QuestionGenerationJob> list(UUID courseId);

    List<Question> generatedQuestions(UUID jobId);

    QuestionGenerationJob generate(UUID courseId, GenerateRequest request);

    AiStatus status();
}
