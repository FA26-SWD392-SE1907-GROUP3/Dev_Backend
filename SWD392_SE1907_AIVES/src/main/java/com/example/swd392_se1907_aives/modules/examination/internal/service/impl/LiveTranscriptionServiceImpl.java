package com.example.swd392_se1907_aives.modules.examination.internal.service.impl;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.LiveTranscriptionDtos.TicketView;
import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewAiClient;
import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewStateStore;
import com.example.swd392_se1907_aives.modules.examination.internal.service.LiveTranscriptionService;
import com.example.swd392_se1907_aives.modules.examination.internal.service.LiveTranscriptionTickets;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LiveTranscriptionServiceImpl implements LiveTranscriptionService {
    private final InterviewStateStore store;
    private final LiveTranscriptionTickets tickets;
    private final InterviewAiClient ai;

    @Override
    public TicketView issueTicket(UUID attemptId, UUID promptId) {
        ai.requireConfigured();
        return new TicketView(
                tickets.issue(store.livePermission(attemptId, promptId)),
                "/api/v1/interviews/live");
    }
}
