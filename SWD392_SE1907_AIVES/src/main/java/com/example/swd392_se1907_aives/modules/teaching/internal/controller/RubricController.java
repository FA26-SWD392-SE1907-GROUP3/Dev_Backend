package com.example.swd392_se1907_aives.modules.teaching.internal.controller;

import com.example.swd392_se1907_aives.modules.teaching.internal.dto.RubricDtos.RubricRequest;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.RubricDtos.RubricView;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.RubricService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class RubricController {
    private final RubricService service;

    @PostMapping("/rubrics")
    @PreAuthorize("hasRole('LECTURER')")
    public RubricView createRubric(@Valid @RequestBody RubricRequest request) {
        return service.createRubric(request);
    }

    @GetMapping("/courses/{courseId}/rubrics")
    @PreAuthorize("hasRole('LECTURER')")
    public List<RubricView> rubrics(@PathVariable UUID courseId) {
        return service.rubrics(courseId);
    }

    @PutMapping("/rubrics/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public RubricView updateRubric(
            @PathVariable("id") UUID id, @Valid @RequestBody RubricRequest request) {
        return service.updateRubric(id, request);
    }

    @DeleteMapping("/rubrics/{id}")
    @PreAuthorize("hasRole('LECTURER')")
    public void removeRubric(@PathVariable("id") UUID id) {
        service.removeRubric(id);
    }

    @PatchMapping("/rubrics/{id}/publish")
    @PreAuthorize("hasRole('LECTURER')")
    public RubricView publish(@PathVariable UUID id) {
        return service.publish(id);
    }
}
