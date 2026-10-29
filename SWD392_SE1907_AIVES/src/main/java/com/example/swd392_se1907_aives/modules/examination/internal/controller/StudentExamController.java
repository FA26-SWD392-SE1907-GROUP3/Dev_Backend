package com.example.swd392_se1907_aives.modules.examination.internal.controller;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.StudentExamDtos.AttemptView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.StudentExamDtos.CandidateView;
import com.example.swd392_se1907_aives.modules.examination.internal.service.StudentExamService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/student")
@PreAuthorize("hasRole('STUDENT')")
public class StudentExamController {
    private final StudentExamService service;

    @GetMapping("/exams")
    public List<CandidateView> myExams() {
        return service.myExams();
    }

    @GetMapping("/exams/{candidateId}/attempts")
    public List<AttemptView> myAttempts(@PathVariable UUID candidateId) {
        return service.myAttempts(candidateId);
    }
}
