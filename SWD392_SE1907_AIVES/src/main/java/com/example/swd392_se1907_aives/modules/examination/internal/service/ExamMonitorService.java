package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.AudioFile;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamMonitorDtos.MonitorView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamMonitorDtos.TranscriptView;

import java.util.UUID;

public interface ExamMonitorService {
    MonitorView monitor(UUID examId);

    TranscriptView transcript(UUID examId, UUID attemptId);

    AudioFile audio(UUID examId, UUID attemptId, UUID turnId);
}
