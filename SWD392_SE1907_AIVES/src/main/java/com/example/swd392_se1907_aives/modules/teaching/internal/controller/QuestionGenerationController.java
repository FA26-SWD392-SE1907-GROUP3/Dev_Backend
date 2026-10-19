package com.example.swd392_se1907_aives.modules.teaching.internal.controller;

import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionGenerationJob;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.OpenAiQuestionClient;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.QuestionGenerationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.*;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1")
@PreAuthorize("hasRole('LECTURER')")
public class QuestionGenerationController {
    private final QuestionGenerationService service;
    private final OpenAiQuestionClient client;
    public record AiStatus(boolean configured,String model) {}
    public record JobView(UUID id,UUID courseId,String status,int generatedCount,int desiredCount,String errorMessage,OffsetDateTime createdAt) {}
    public record GeneratedQuestionView(UUID id,UUID rubricId,String content,String referenceAnswer,String bloomLevel,String difficulty,String status){}
    private JobView view(QuestionGenerationJob j){return new JobView(j.getId(),j.getCourseId(),j.getStatus(),j.getGeneratedCount(),j.getDesiredCount(),j.getErrorMessage(),j.getCreatedAt());}
    @GetMapping("/ai/status") public AiStatus status(){return new AiStatus(client.configured(),client.model());}
    @GetMapping("/courses/{courseId}/generation-jobs") public List<JobView> list(@PathVariable UUID courseId){return service.list(courseId).stream().map(this::view).toList();}
    @GetMapping("/generation-jobs/{jobId}/questions") public List<GeneratedQuestionView> generated(@PathVariable UUID jobId){return service.generatedQuestions(jobId).stream().map(q->new GeneratedQuestionView(q.getId(),q.getRubricId(),q.getContent(),q.getReferenceAnswer(),q.getBloomLevel(),q.getDifficulty(),q.getStatus())).toList();}
    @PostMapping("/courses/{courseId}/generation-jobs") @ResponseStatus(HttpStatus.CREATED)
    public JobView generate(@PathVariable UUID courseId,@Valid @RequestBody QuestionGenerationService.GenerateRequest request){return view(service.generate(courseId,request));}
}
