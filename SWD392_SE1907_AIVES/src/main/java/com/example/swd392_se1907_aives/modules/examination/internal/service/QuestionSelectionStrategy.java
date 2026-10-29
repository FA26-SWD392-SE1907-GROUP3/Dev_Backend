package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.example.swd392_se1907_aives.modules.teaching.api.QuestionInfo;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Interchangeable algorithm; the context validates the requested count first. */
public interface QuestionSelectionStrategy {
    String mode();

    List<QuestionInfo> select(
            List<QuestionInfo> eligible, int count,
            Set<UUID> previous, Map<UUID, Integer> usage);
}
