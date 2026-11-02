package com.example.swd392_se1907_aives.modules.examination.internal.dto;

import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewAiClient;

/** Provider contract. Only a validated message may reach captions or speech synthesis. */
public record InterviewDecision(Decision decision, Reason reason, String message) {
    public enum Decision { FOLLOW_UP, NEXT }
    public enum Reason { VAGUE, MISSING, CONTRADICTION, SUFFICIENT }

    public InterviewDecision {
        if (decision == null || reason == null || message == null)
            throw new IllegalArgumentException("Missing interview decision field");
        message = message.strip();
        long questions = message.chars().filter(c -> c == '?').count();
        if (message.isEmpty() || message.length() > 2000
                || decision == Decision.FOLLOW_UP && (reason == Reason.SUFFICIENT || questions != 1 || !message.endsWith("?"))
                || decision == Decision.NEXT && (reason != Reason.SUFFICIENT || questions != 0))
            throw new IllegalArgumentException("Invalid interview decision");
    }

    public InterviewAiClient.Decision toInternal() {
        return decision == Decision.FOLLOW_UP
                ? new InterviewAiClient.Decision(true, message, reason.name())
                : new InterviewAiClient.Decision(false, "", reason.name(), message);
    }
}
