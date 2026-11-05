package com.example.swd392_se1907_aives.modules.examination.internal.controller;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.AttemptGradingDtos.GradingView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.AttemptGradingDtos.PublishRequest;
import com.example.swd392_se1907_aives.modules.examination.internal.service.AttemptGradingService;

import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/attempts/{attemptId}/grading")
@PreAuthorize("hasAnyRole('LECTURER','STUDENT','ADMIN')")
public class AttemptGradingController {
    private final AttemptGradingService service;

    @GetMapping
    public GradingView get(@PathVariable UUID attemptId) {
        return service.get(attemptId);
    }

    @PostMapping("/retry")
    @PreAuthorize("hasRole('LECTURER')")
    public GradingView retry(@PathVariable UUID attemptId) {
        return service.retry(attemptId);
    }

    @PostMapping("/publish")
    @PreAuthorize("hasRole('LECTURER')")
    public GradingView publish(@PathVariable UUID attemptId, @Valid @RequestBody PublishRequest request) {
        return service.publish(attemptId, request);
    }
}
