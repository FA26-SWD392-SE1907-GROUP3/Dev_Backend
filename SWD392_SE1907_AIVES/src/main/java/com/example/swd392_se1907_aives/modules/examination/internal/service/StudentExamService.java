package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.StudentExamDtos.AttemptView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.StudentExamDtos.CandidateView;

import java.util.List;
import java.util.UUID;

public interface StudentExamService {
    List<CandidateView> myExams();

    List<AttemptView> myAttempts(UUID candidateId);
}
