package com.example.swd392_se1907_aives.modules.examination.internal.controller;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamMonitorDtos.MonitorView;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.ExamMonitorDtos.TranscriptView;
import com.example.swd392_se1907_aives.modules.examination.internal.service.ExamMonitorService;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/exams/{examId}")
@PreAuthorize("hasRole('LECTURER')")
public class ExamMonitorController {
    private final ExamMonitorService service;

    @GetMapping("/monitor")
    public MonitorView monitor(@PathVariable("examId") UUID examId) {
        return service.monitor(examId);
    }

    @GetMapping("/attempts/{attemptId}/transcript")
    public TranscriptView transcript(
            @PathVariable("examId") UUID examId, @PathVariable("attemptId") UUID attemptId) {
        return service.transcript(examId, attemptId);
    }

    @GetMapping("/attempts/{attemptId}/audio/{turnId}")
    public ResponseEntity<Resource> audio(
            @PathVariable("examId") UUID examId,
            @PathVariable("attemptId") UUID attemptId,
            @PathVariable("turnId") UUID turnId) {
        var recording = service.audio(examId, attemptId, turnId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(recording.contentType()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(recording.path()));
    }
}
