package com.example.swd392_se1907_aives.modules.examination.internal.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LiveTranscriptionTickets {
    public record Ticket(UUID promptId, UUID studentId, Instant deadline, Instant expiresAt) {}

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public String issue(InterviewStateStore.LivePermission permission) {
        Instant now = Instant.now();
        tickets.entrySet()
                .removeIf(
                        e ->
                                !e.getValue().expiresAt().isAfter(now)
                                        || e.getValue().promptId().equals(permission.promptId()));
        String token = UUID.randomUUID().toString() + UUID.randomUUID();
        tickets.put(
                token,
                new Ticket(
                        permission.promptId(),
                        permission.studentId(),
                        permission.deadline().toInstant(),
                        now.plusSeconds(30)));
        return token;
    }

    public Ticket consume(String token) {
        Ticket ticket = token == null ? null : tickets.remove(token);
        if (ticket == null
                || !ticket.expiresAt().isAfter(Instant.now())
                || !ticket.deadline().isAfter(Instant.now()))
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Phiên micro không hợp lệ hoặc đã hết hạn.");
        return ticket;
    }
}
