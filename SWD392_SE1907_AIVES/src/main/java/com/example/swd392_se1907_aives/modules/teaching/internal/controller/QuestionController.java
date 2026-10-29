package com.example.swd392_se1907_aives.modules.teaching.internal.controller;

import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionDtos.QuestionRequest;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionDtos.QuestionView;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.QuestionService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class QuestionController {
    private final QuestionService service;

    @PostMapping("/questions")
    @PreAuthorize("hasRole('LECTURER')")
    public QuestionView createQuestion(@Valid @RequestBody QuestionRequest request) {
        return service.createQuestion(request);
    }

    @GetMapping("/courses/{courseId}/questions")
    @PreAuthorize("hasRole('LECTURER')")
    public List<QuestionView> list(@PathVariable UUID courseId) {
        return service.list(courseId);
    }

    @PatchMapping("/questions/{id}/review")
    @PreAuthorize("hasRole('LECTURER')")
    public QuestionView review(
            @PathVariable UUID id,
            @RequestParam boolean approve,
            @RequestBody(required = false) String comment) {
        return service.review(id, approve, comment);
    }

    @PutMapping("/questions/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public QuestionView updateQuestion(
            @PathVariable UUID id, @Valid @RequestBody QuestionRequest request) {
        return service.updateQuestion(id, request);
    }

    @DeleteMapping("/questions/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public QuestionView archiveQuestion(@PathVariable UUID id) {
        return service.archiveQuestion(id);
    }
}
