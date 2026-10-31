package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.AttemptGradingDtos.GradingView;

import java.util.UUID;

public interface AttemptGradingService {
    GradingView get(UUID attemptId);

    GradingView retry(UUID attemptId);

    void processPending();
}
