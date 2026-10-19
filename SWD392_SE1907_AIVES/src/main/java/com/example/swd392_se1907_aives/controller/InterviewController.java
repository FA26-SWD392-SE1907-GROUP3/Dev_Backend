package com.example.swd392_se1907_aives.controller;
import com.example.swd392_se1907_aives.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/interviews") @PreAuthorize("hasRole('STUDENT')")
public class InterviewController {
    private final InterviewService service;
    private final InterviewStateStore store;
    private final InterviewAudioStorage audio;
    public record PromptRequest(@NotNull UUID promptId) {}
    public record AnswerRequest(@NotNull UUID promptId,@NotBlank @Size(max=12000) String text) {}
    public record ContinueRequest(@NotNull UUID answerTurnId) {}
    @GetMapping("/{id}") public InterviewStateStore.State state(@PathVariable UUID id){return service.state(id);}
    @PostMapping("/{id}/begin") public InterviewStateStore.State begin(@PathVariable UUID id,@Valid @RequestBody PromptRequest request){return store.begin(id,request.promptId());}
    @PostMapping("/{id}/answers") public InterviewStateStore.State answer(@PathVariable UUID id,@Valid @RequestBody AnswerRequest request){return service.textAnswer(id,request.promptId(),request.text());}
    @PostMapping(value="/{id}/audio-answers",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public InterviewStateStore.State audioAnswer(@PathVariable UUID id,@RequestParam UUID promptId,@RequestParam("file") MultipartFile file){return service.audioAnswer(id,promptId,file);}
    @PostMapping("/{id}/continue") public InterviewStateStore.State next(@PathVariable UUID id,@Valid @RequestBody ContinueRequest request){return service.continueAnswer(id,request.answerTurnId());}
    @PostMapping("/{id}/timeout") public InterviewStateStore.State timeout(@PathVariable UUID id,@Valid @RequestBody PromptRequest request){return store.timeout(id,request.promptId());}
    @GetMapping("/{id}/speech/{turnId}") public ResponseEntity<byte[]> speech(@PathVariable UUID id,@PathVariable UUID turnId){
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/wav")).cacheControl(CacheControl.noStore()).body(service.speech(id,turnId));
    }
    @GetMapping("/{id}/audio/{turnId}") public ResponseEntity<Resource> audio(@PathVariable UUID id,@PathVariable UUID turnId){
        String key=store.audioKey(id,turnId);var path=audio.path(key);
        String mime=key.endsWith(".wav")?"audio/wav":key.endsWith(".ogg")?"audio/ogg":key.endsWith(".mp4")?"audio/mp4":"audio/webm";
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(mime)).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(new FileSystemResource(path));
    }
}
