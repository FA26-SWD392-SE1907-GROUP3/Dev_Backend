package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.CandidateCreate;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.ExamCreate;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.ExamView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.QuestionId;

import java.util.List;
import java.util.UUID;

public interface ExamService {
    ExamView create(ExamCreate request);

    void addQuestion(UUID id, QuestionId request);

    UUID assign(UUID id, CandidateCreate request);

    ExamView activate(UUID id);

    List<ExamView> list();

    ExamView one(UUID id);
}
