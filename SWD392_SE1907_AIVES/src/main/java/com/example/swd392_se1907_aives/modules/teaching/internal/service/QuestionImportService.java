package com.example.swd392_se1907_aives.modules.teaching.internal.service;

import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionImportDtos.Result;

import java.util.UUID;

public interface QuestionImportService {
    Result process(UUID courseId, String filename, byte[] bytes, boolean preview);
}
