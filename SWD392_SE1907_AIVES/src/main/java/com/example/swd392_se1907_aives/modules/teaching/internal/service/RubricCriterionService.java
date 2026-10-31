package com.example.swd392_se1907_aives.modules.teaching.internal.service;

import com.example.swd392_se1907_aives.modules.teaching.internal.dto.RubricCriterionDtos.CriterionRequest;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.RubricCriterionDtos.CriterionView;

import java.util.List;
import java.util.UUID;

public interface RubricCriterionService {
    List<CriterionView> criteria(UUID id);

    CriterionView addCriterion(UUID id, CriterionRequest request);

    CriterionView updateCriterion(UUID id, UUID criterionId, CriterionRequest request);

    void deleteCriterion(UUID id, UUID criterionId);
}
