package com.example.swd392_se1907_aives.modules.examination.internal.controller;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.CandidateCreate;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.ExamCreate;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.ExamView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamDtos.QuestionId;
import com.example.swd392_se1907_aives.modules.examination.internal.service.ExamService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/exams")
public class ExamController {
    private final ExamService service;

    @PostMapping
    @PreAuthorize("hasRole('LECTURER')")
    public ExamView create(@Valid @RequestBody ExamCreate request) {
        return service.create(request);
    }

    @PostMapping("/{id}/questions")
    @PreAuthorize("hasRole('LECTURER')")
    public void addQuestion(@PathVariable UUID id, @Valid @RequestBody QuestionId request) {
        service.addQuestion(id, request);
    }

    @PostMapping("/{id}/candidates")
    @PreAuthorize("hasRole('LECTURER')")
    public UUID assign(@PathVariable UUID id, @Valid @RequestBody CandidateCreate request) {
        return service.assign(id, request);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('LECTURER')")
    public ExamView activate(@PathVariable UUID id) {
        return service.activate(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('LECTURER')")
    public List<ExamView> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public ExamView one(@PathVariable UUID id) {
        return service.one(id);
    }
}
