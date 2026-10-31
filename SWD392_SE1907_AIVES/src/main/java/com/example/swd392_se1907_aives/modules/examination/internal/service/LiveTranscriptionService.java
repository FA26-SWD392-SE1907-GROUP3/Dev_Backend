package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.LiveTranscriptionDtos.TicketView;

import java.util.UUID;

public interface LiveTranscriptionService {
    TicketView issueTicket(UUID attemptId, UUID promptId);
}
