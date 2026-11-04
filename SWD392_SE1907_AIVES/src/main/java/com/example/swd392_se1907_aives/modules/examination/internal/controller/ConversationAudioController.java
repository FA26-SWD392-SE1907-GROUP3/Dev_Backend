package com.example.swd392_se1907_aives.modules.examination.internal.controller;

import com.example.swd392_se1907_aives.modules.examination.internal.service.ConversationAudioService;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.InterviewDtos.PromptRequest;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.LiveTranscriptionDtos.TicketView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/interviews")
@PreAuthorize("hasRole('STUDENT')")
public class ConversationAudioController {
    private final ConversationAudioService service;
    public record VoiceRequest(UUID promptId,@NotNull ConversationAudioService.Kind kind){}
    @PostMapping("/{id}/voice")
    public ResponseEntity<byte[]> voice(@PathVariable UUID id,@Valid @RequestBody VoiceRequest request){
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/wav"))
                .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff")
                .body(service.voice(id,request.promptId(),request.kind()));
    }
    @PostMapping("/{id}/voice-ticket")
    public TicketView ticket(@PathVariable UUID id,@Valid @RequestBody PromptRequest request){return service.ticket(id,request.promptId());}
    @PostMapping(value="/{id}/transcribe",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String,String> transcribe(@PathVariable UUID id,@RequestParam UUID promptId,@RequestParam("file") MultipartFile file){
        return Map.of("text",service.transcribe(id,promptId,file));
    }
}
