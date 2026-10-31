package com.example.swd392_se1907_aives.modules.teaching.internal.service;

import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionDetailDtos.DetailView;

import java.util.UUID;

public interface QuestionDetailService {
    DetailView detail(UUID id);
}
