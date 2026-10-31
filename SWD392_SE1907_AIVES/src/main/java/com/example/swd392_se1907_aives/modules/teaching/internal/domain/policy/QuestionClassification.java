package com.example.swd392_se1907_aives.modules.teaching.internal.domain.policy;

import java.util.Set;

public final class QuestionClassification {
    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");

    private QuestionClassification() {}

    public static boolean isValidDifficulty(String value) {
        return value != null && DIFFICULTIES.contains(value);
    }
}
