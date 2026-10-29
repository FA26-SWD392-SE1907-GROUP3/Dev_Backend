package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.AttemptDtos.AnswerRequest;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.AttemptDtos.AssignedQuestion;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.AttemptDtos.AttemptView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.InterviewDtos.State;

import java.util.List;
import java.util.UUID;

public interface AttemptService {
    AttemptView start(UUID candidateId);

    List<AssignedQuestion> assigned(UUID id);

    State answer(UUID assignedQuestionId, AnswerRequest request);
}
