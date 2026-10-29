package com.example.swd392_se1907_aives.modules.teaching.internal.service;

import com.example.swd392_se1907_aives.modules.teaching.internal.dto.RubricDtos.RubricRequest;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.RubricDtos.RubricView;

import java.util.List;
import java.util.UUID;

public interface RubricService {
    RubricView createRubric(RubricRequest request);

    List<RubricView> rubrics(UUID courseId);

    RubricView updateRubric(UUID id, RubricRequest request);

    void removeRubric(UUID id);

    RubricView publish(UUID id);
}
