package com.example.swd392_se1907_aives.modules.examination.internal.controller;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.InterviewDtos.PromptRequest;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.LiveTranscriptionDtos.TicketView;
import com.example.swd392_se1907_aives.modules.examination.internal.service.LiveTranscriptionService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/interviews")
@PreAuthorize("hasRole('STUDENT')")
public class LiveTranscriptionController {
    private final LiveTranscriptionService service;

    @PostMapping("/{id}/live-ticket")
    public TicketView ticket(
            @PathVariable java.util.UUID id, @Valid @RequestBody PromptRequest request) {
        return service.issueTicket(id, request.promptId());
    }
}
