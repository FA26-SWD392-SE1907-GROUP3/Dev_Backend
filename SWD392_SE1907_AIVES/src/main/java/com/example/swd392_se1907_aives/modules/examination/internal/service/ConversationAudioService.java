package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.LiveTranscriptionDtos.TicketView;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

public interface ConversationAudioService {
    enum Kind { GREETING, PROMPT, REPLAY, REMINDER, CLOSING }
    byte[] voice(UUID attemptId, UUID promptId, Kind kind);
    TicketView ticket(UUID attemptId, UUID promptId);
    String transcribe(UUID attemptId, UUID promptId, MultipartFile file);
}
