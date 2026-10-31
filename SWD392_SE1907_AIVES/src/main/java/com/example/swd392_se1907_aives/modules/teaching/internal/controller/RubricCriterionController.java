package com.example.swd392_se1907_aives.modules.teaching.internal.controller;

import com.example.swd392_se1907_aives.modules.teaching.internal.dto.RubricCriterionDtos.CriterionRequest;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.RubricCriterionDtos.CriterionView;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.RubricCriterionService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
public class RubricCriterionController {
    private final RubricCriterionService service;

    @GetMapping("/rubrics/{id}/criteria")
    @PreAuthorize("hasRole('LECTURER')")
    public List<CriterionView> criteria(@PathVariable UUID id) {
        return service.criteria(id);
    }

    @PostMapping("/rubrics/{id}/criteria")
    @PreAuthorize("hasRole('LECTURER')")
    public CriterionView addCriterion(
            @PathVariable UUID id, @Valid @RequestBody CriterionRequest request) {
        return service.addCriterion(id, request);
    }

    @PutMapping("/rubrics/{id}/criteria/{criterionId}")
    @PreAuthorize("hasRole('LECTURER')")
    public CriterionView updateCriterion(
            @PathVariable UUID id,
            @PathVariable UUID criterionId,
            @Valid @RequestBody CriterionRequest request) {
        return service.updateCriterion(id, criterionId, request);
    }

    @DeleteMapping("/rubrics/{id}/criteria/{criterionId}")
    @PreAuthorize("hasRole('LECTURER')")
    public void deleteCriterion(@PathVariable UUID id, @PathVariable UUID criterionId) {
        service.deleteCriterion(id, criterionId);
    }
}
