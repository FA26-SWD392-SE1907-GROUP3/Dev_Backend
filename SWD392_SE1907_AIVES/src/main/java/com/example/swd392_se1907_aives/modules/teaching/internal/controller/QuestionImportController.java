package com.example.swd392_se1907_aives.modules.teaching.internal.controller;

import com.example.swd392_se1907_aives.modules.teaching.internal.service.QuestionImportService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/courses/{courseId}/question-imports") @PreAuthorize("hasRole('LECTURER')")
public class QuestionImportController {
    private final QuestionImportService service;
    @PostMapping(consumes="multipart/form-data")
    public QuestionImportService.Result upload(@PathVariable UUID courseId,@RequestParam("file") MultipartFile file,@RequestParam(required=false) UUID rubricId,@RequestParam(defaultValue="true") boolean preview)throws java.io.IOException{
        return service.process(courseId,rubricId,java.util.Objects.toString(file.getOriginalFilename(),"questions.csv"),file.getBytes(),preview);
    }
}
