package com.example.swd392_se1907_aives.modules.examination.internal.controller;

import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewAiClient;
import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewStateStore;
import com.example.swd392_se1907_aives.modules.examination.internal.service.LiveTranscriptionTickets;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/interviews") @PreAuthorize("hasRole('STUDENT')")
public class LiveTranscriptionController {
    private final InterviewStateStore store;
    private final LiveTranscriptionTickets tickets;
    private final InterviewAiClient ai;
    public record TicketView(String ticket,String path) {}
    @PostMapping("/{id}/live-ticket") public TicketView ticket(@PathVariable java.util.UUID id,@Valid @RequestBody InterviewController.PromptRequest request){
        ai.requireConfigured();return new TicketView(tickets.issue(store.livePermission(id,request.promptId())),"/api/v1/interviews/live");
    }
}
