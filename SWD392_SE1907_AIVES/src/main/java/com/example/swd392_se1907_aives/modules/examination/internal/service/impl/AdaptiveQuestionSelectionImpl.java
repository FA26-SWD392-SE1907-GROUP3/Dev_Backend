package com.example.swd392_se1907_aives.modules.examination.internal.service.impl;

import com.example.swd392_se1907_aives.modules.examination.internal.service.QuestionSelectionStrategy;
import com.example.swd392_se1907_aives.modules.teaching.api.QuestionInfo;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Balances exposure and difficulty coverage; does not infer student ability. */
@Service
public class AdaptiveQuestionSelectionImpl implements QuestionSelectionStrategy {
    private final Random random;

    public AdaptiveQuestionSelectionImpl() {
        this(new Random());
    }

    public AdaptiveQuestionSelectionImpl(Random random) {
        this.random = random;
    }

    @Override
    public String mode() {
        return "ADAPTIVE";
    }

    @Override
    public List<QuestionInfo> select(
            List<QuestionInfo> eligible, int count,
            Set<UUID> previous, Map<UUID, Integer> usage) {
        var remaining = new ArrayList<>(eligible);
        Collections.shuffle(remaining, random);
        List<QuestionInfo> selected = new ArrayList<>();
        Map<String, Integer> difficulties = new HashMap<>();
        for (int i = 0; i < count; i++) {
            // Stable sort retains randomized ties, with the same priorities as before.
            remaining.sort(
                    Comparator.<QuestionInfo, Boolean>comparing(q -> previous.contains(q.getId()))
                            .thenComparingInt(q -> usage.getOrDefault(q.getId(), 0))
                            .thenComparingInt(q -> difficulties.getOrDefault(q.getDifficulty(), 0)));
            QuestionInfo question = remaining.removeFirst();
            selected.add(question);
            difficulties.merge(question.getDifficulty(), 1, Integer::sum);
        }
        return selected;
    }
}
