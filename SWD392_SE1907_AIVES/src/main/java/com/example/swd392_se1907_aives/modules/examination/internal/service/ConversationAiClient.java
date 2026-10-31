package com.example.swd392_se1907_aives.modules.examination.internal.service;

import java.util.function.Consumer;

public interface ConversationAiClient {
    InterviewAiClient.Decision stream(String context, Consumer<String> onText);
}
