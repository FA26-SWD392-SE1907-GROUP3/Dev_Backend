package com.example.swd392_se1907_aives.modules.teaching.internal.controller;

import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionGenerationJob;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionGenerationDtos.AiStatus;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionGenerationDtos.GenerateRequest;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionGenerationDtos.GeneratedQuestionView;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionGenerationDtos.JobView;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.QuestionGenerationService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('LECTURER')")
public class QuestionGenerationController {
    private final QuestionGenerationService service;

    private JobView view(QuestionGenerationJob j) {
        return new JobView(
                j.getId(),
                j.getCourseId(),
                j.getStatus(),
                j.getGeneratedCount(),
                j.getDesiredCount(),
                j.getErrorMessage(),
                j.getCreatedAt());
    }

    @GetMapping("/ai/status")
    public AiStatus status() {
        return service.status();
    }

    @GetMapping("/courses/{courseId}/generation-jobs")
    public List<JobView> list(@PathVariable UUID courseId) {
        return service.list(courseId).stream().map(this::view).toList();
    }

    @GetMapping("/generation-jobs/{jobId}/questions")
    public List<GeneratedQuestionView> generated(@PathVariable UUID jobId) {
        return service.generatedQuestions(jobId).stream()
                .map(
                        q ->
                                new GeneratedQuestionView(
                                        q.getId(),
                                        q.getContent(),
                                        q.getReferenceAnswer(),
                                        q.getDifficulty(),
                                        q.getStatus()))
                .toList();
    }

    @PostMapping("/courses/{courseId}/generation-jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public JobView generate(
            @PathVariable UUID courseId, @Valid @RequestBody GenerateRequest request) {
        return view(service.generate(courseId, request));
    }
}
