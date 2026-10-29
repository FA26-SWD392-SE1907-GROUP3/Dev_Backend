package com.example.swd392_se1907_aives.modules.examination.internal.service.impl;

import com.example.swd392_se1907_aives.modules.examination.internal.service.QuestionSelectionStrategy;
import com.example.swd392_se1907_aives.modules.teaching.api.QuestionInfo;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Service
public class RandomQuestionSelectionImpl implements QuestionSelectionStrategy {
    private final Random random;

    public RandomQuestionSelectionImpl() {
        this(new Random());
    }

    /** A fixed seed makes the presentation repeatable. Production uses the no-arg constructor. */
    public RandomQuestionSelectionImpl(Random random) {
        this.random = random;
    }

    @Override
    public String mode() {
        return "RANDOM";
    }

    @Override
    public List<QuestionInfo> select(
            List<QuestionInfo> eligible, int count,
            Set<UUID> previous, Map<UUID, Integer> usage) {
        var remaining = new ArrayList<>(eligible);
        Collections.shuffle(remaining, random);
        remaining.sort(Comparator.comparing(q -> previous.contains(q.getId())));
        return new ArrayList<>(remaining.subList(0, count));
    }
}
