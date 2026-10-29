package com.example.swd392_se1907_aives.modules.teaching.internal.controller;

import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionDetailDtos.DetailView;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.QuestionDetailService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/questions")
@PreAuthorize("hasRole('LECTURER')")
public class QuestionDetailController {
    private final QuestionDetailService service;

    @GetMapping("/{id}")
    public DetailView detail(@PathVariable UUID id) {
        return service.detail(id);
    }
}
