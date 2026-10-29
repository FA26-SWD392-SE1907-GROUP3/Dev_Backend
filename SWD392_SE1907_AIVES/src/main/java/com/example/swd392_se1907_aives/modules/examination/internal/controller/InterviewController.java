package com.example.swd392_se1907_aives.modules.examination.internal.controller;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.InterviewDtos.AnswerRequest;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.InterviewDtos.ContinueRequest;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.InterviewDtos.PromptRequest;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.InterviewDtos.State;
import com.example.swd392_se1907_aives.modules.examination.internal.service.InterviewService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/interviews")
@PreAuthorize("hasRole('STUDENT')")
public class InterviewController {
    private final InterviewService service;

    @GetMapping("/{id}")
    public State state(@PathVariable UUID id) {
        return service.state(id);
    }

    @PostMapping("/{id}/begin")
    public State begin(@PathVariable UUID id, @Valid @RequestBody PromptRequest request) {
        return service.begin(id, request.promptId());
    }

    @PostMapping("/{id}/answers")
    public State answer(@PathVariable UUID id, @Valid @RequestBody AnswerRequest request) {
        return service.textAnswer(id, request.promptId(), request.text());
    }

    @PostMapping(value = "/{id}/audio-answers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public State audioAnswer(
            @PathVariable UUID id,
            @RequestParam UUID promptId,
            @RequestParam("file") MultipartFile file) {
        return service.audioAnswer(id, promptId, file);
    }

    @PostMapping("/{id}/continue")
    public State next(@PathVariable UUID id, @Valid @RequestBody ContinueRequest request) {
        return service.continueAnswer(id, request.answerTurnId());
    }

    @PostMapping("/{id}/timeout")
    public State timeout(@PathVariable UUID id, @Valid @RequestBody PromptRequest request) {
        return service.timeout(id, request.promptId());
    }

    @GetMapping("/{id}/speech/{turnId}")
    public ResponseEntity<byte[]> speech(@PathVariable UUID id, @PathVariable UUID turnId) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/wav"))
                .cacheControl(CacheControl.noStore())
                .body(service.speech(id, turnId));
    }

    @GetMapping("/{id}/audio/{turnId}")
    public ResponseEntity<Resource> audio(@PathVariable UUID id, @PathVariable UUID turnId) {
        var recording = service.audio(id, turnId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(recording.contentType()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(recording.path()));
    }
}
