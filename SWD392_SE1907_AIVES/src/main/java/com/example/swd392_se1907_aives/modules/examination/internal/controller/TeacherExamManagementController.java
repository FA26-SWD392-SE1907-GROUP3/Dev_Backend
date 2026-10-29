package com.example.swd392_se1907_aives.modules.examination.internal.controller;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.ExamCreate;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.ExamView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.TeacherExamManagementDtos.CandidateView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.TeacherExamManagementDtos.PoolView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.TeacherExamManagementDtos.Schedule;
import com.example.swd392_se1907_aives.modules.examination.internal.service.TeacherExamManagementService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/exams")
@PreAuthorize("hasRole('LECTURER')")
public class TeacherExamManagementController {
    private final TeacherExamManagementService service;

    @GetMapping("/{id}/questions")
    public List<PoolView> pool(@PathVariable UUID id) {
        return service.pool(id);
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    public void removeQuestion(@PathVariable UUID id, @PathVariable UUID questionId) {
        service.removeQuestion(id, questionId);
    }

    @PutMapping("/{id}")
    public ExamView update(@PathVariable UUID id, @Valid @RequestBody ExamCreate request) {
        return service.update(id, request);
    }

    @GetMapping("/{id}/candidates")
    public List<CandidateView> candidates(@PathVariable UUID id) {
        return service.candidates(id);
    }

    @PutMapping("/{id}/candidates/{candidateId}")
    public CandidateView reschedule(
            @PathVariable UUID id,
            @PathVariable UUID candidateId,
            @Valid @RequestBody Schedule request) {
        return service.reschedule(id, candidateId, request);
    }

    @DeleteMapping("/{id}/candidates/{candidateId}")
    public void cancelCandidate(@PathVariable UUID id, @PathVariable UUID candidateId) {
        service.cancelCandidate(id, candidateId);
    }

    @DeleteMapping("/{id}")
    public void removeDraft(@PathVariable("id") UUID id) {
        service.removeDraft(id);
    }

    @PatchMapping("/{id}/close")
    public ExamView close(@PathVariable UUID id) {
        return service.close(id);
    }
}
