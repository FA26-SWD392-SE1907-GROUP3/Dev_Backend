package com.example.swd392_se1907_aives.controller;
import com.example.swd392_se1907_aives.service.InterviewService;
import com.example.swd392_se1907_aives.dto.ApiModels.Answer;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/interviews/{scheduleId}")
@RequiredArgsConstructor
public class InterviewController {
 private final InterviewService service;
 @GetMapping public Object state(@PathVariable Integer scheduleId) { return service.state(scheduleId); }
 @PostMapping("/start") @PreAuthorize("hasRole('STUDENT')") public Object start(@PathVariable Integer scheduleId) { return service.start(scheduleId); }
 @PostMapping("/answer") @PreAuthorize("hasRole('STUDENT')") public Object answer(@PathVariable Integer scheduleId,@Valid @RequestBody Answer input) { return service.answer(scheduleId,input); }
 @PostMapping("/next") @PreAuthorize("hasRole('STUDENT')") public Object next(@PathVariable Integer scheduleId,@RequestParam Integer afterLogId) { return service.next(scheduleId,afterLogId); }
 @PostMapping("/finish") @PreAuthorize("hasRole('STUDENT')") public Object finish(@PathVariable Integer scheduleId) { return service.finish(scheduleId); }
}
