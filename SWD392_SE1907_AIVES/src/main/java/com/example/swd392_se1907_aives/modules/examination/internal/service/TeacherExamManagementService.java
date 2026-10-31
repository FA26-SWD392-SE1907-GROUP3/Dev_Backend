package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.ExamCreate;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.ExamView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.TeacherExamManagementDtos.CandidateView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.TeacherExamManagementDtos.PoolView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.TeacherExamManagementDtos.Schedule;

import java.util.List;
import java.util.UUID;

public interface TeacherExamManagementService {
    List<PoolView> pool(UUID id);

    void removeQuestion(UUID id, UUID questionId);

    ExamView update(UUID id, ExamCreate request);

    List<CandidateView> candidates(UUID id);

    CandidateView reschedule(UUID id, UUID candidateId, Schedule request);

    void cancelCandidate(UUID id, UUID candidateId);

    void removeDraft(UUID id);

    ExamView close(UUID id);
}
