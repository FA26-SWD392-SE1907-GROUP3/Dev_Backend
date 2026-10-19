package com.example.swd392_se1907_aives.service;

import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewStateStore;
import com.example.swd392_se1907_aives.modules.examination.internal.service.LiveTranscriptionTickets;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class LiveTranscriptionTicketsTests {
    @Test void ticketCanOnlyBeUsedOnceAndANewTicketRevokesTheOldOne(){
        var service=new LiveTranscriptionTickets();var permission=new InterviewStateStore.LivePermission(UUID.randomUUID(),UUID.randomUUID(),OffsetDateTime.now().plusMinutes(1));
        String first=service.issue(permission),second=service.issue(permission);
        assertThatThrownBy(()->service.consume(first)).isInstanceOf(ResponseStatusException.class);
        assertThat(service.consume(second).promptId()).isEqualTo(permission.promptId());
        assertThatThrownBy(()->service.consume(second)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void ticketCannotOpenAudioAfterTheAnswerDeadline(){
        var service=new LiveTranscriptionTickets();String ticket=service.issue(new InterviewStateStore.LivePermission(UUID.randomUUID(),UUID.randomUUID(),OffsetDateTime.now().minusSeconds(1)));
        assertThatThrownBy(()->service.consume(ticket)).isInstanceOf(ResponseStatusException.class);
    }
}
