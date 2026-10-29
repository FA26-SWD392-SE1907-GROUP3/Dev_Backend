package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.AudioFile;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.InterviewDtos.State;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface InterviewService {
    State state(UUID id);

    State textAnswer(UUID id, UUID promptId, String text);

    State audioAnswer(UUID id, UUID promptId, MultipartFile file);

    State continueAnswer(UUID id, UUID answerId);

    byte[] speech(UUID id, UUID turnId);

    State begin(UUID id, UUID promptId);

    State timeout(UUID id, UUID promptId);

    AudioFile audio(UUID id, UUID turnId);
}
