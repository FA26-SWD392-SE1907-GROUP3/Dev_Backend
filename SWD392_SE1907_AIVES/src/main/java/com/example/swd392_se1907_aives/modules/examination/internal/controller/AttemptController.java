package com.example.swd392_se1907_aives.modules.examination.internal.controller;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.AttemptDtos.AnswerRequest;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.AttemptDtos.AssignedQuestion;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.AttemptDtos.AttemptView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.InterviewDtos.State;
import com.example.swd392_se1907_aives.modules.examination.internal.service.AttemptService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/attempts")
@PreAuthorize("hasRole('STUDENT')")
public class AttemptController {
    private final AttemptService service;

    @PostMapping("/start/{candidateId}")
    public AttemptView start(@PathVariable UUID candidateId) {
        return service.start(candidateId);
    }

    @GetMapping("/{id}/questions")
    public List<AssignedQuestion> assigned(@PathVariable UUID id) {
        return service.assigned(id);
    }

    @PostMapping("/questions/{assignedQuestionId}/answer")
    public State answer(
            @PathVariable UUID assignedQuestionId, @Valid @RequestBody AnswerRequest request) {
        return service.answer(assignedQuestionId, request);
    }
}
