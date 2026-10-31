package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.InterviewDtos.State;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.UUID;

public interface InterviewConversationService {
    SseEmitter answer(UUID attemptId, UUID promptId, String text);
    SseEmitter resume(UUID attemptId, UUID answerTurnId);
    State finish(UUID attemptId);
}
