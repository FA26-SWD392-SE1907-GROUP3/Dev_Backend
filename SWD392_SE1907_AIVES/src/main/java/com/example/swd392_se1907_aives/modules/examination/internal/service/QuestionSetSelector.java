package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.example.swd392_se1907_aives.modules.teaching.api.QuestionInfo;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Strategy context. Spring supplies all available selection algorithms. */
@Service
public class QuestionSetSelector {
    private final Map<String, QuestionSelectionStrategy> strategies;

    public QuestionSetSelector(List<QuestionSelectionStrategy> strategies) {
        this.strategies = strategies.stream().collect(Collectors.toUnmodifiableMap(
                QuestionSelectionStrategy::mode, Function.identity()));
        if (!this.strategies.containsKey("RANDOM"))
            throw new IllegalArgumentException("RANDOM strategy is required");
    }

    public List<QuestionInfo> select(
            List<QuestionInfo> eligible,
            int count,
            String mode,
            Set<UUID> previous,
            Map<UUID, Integer> usage) {
        if (count < 1 || eligible.size() < count)
            throw new IllegalArgumentException("Insufficient approved questions");
        // Preserve the previous behavior: null/unknown modes fall back to RANDOM.
        QuestionSelectionStrategy strategy = mode == null
                ? strategies.get("RANDOM")
                : strategies.getOrDefault(mode, strategies.get("RANDOM"));
        return strategy.select(eligible, count, previous, usage);
    }
}
