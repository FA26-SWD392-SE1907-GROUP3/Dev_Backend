package com.example.swd392_se1907_aives.service;

import com.example.swd392_se1907_aives.modules.examination.internal.service.QuestionSetSelector;
import com.example.swd392_se1907_aives.modules.examination.internal.service.impl.AdaptiveQuestionSelectionImpl;
import com.example.swd392_se1907_aives.modules.examination.internal.service.impl.RandomQuestionSelectionImpl;
import com.example.swd392_se1907_aives.modules.teaching.api.QuestionInfo;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Run main() in IntelliJ. Uses real production strategies, without Spring, database or AI. */
public class StrategyPatternDemo {
    public static void main(String[] args) {
        var selector = new QuestionSetSelector(List.of(
                new RandomQuestionSelectionImpl(new Random(42)),
                new AdaptiveQuestionSelectionImpl(new Random(42))));
        var questions = List.of(
                question(1, "Define a class", "EASY"),
                question(2, "Explain encapsulation", "EASY"),
                question(3, "Compare interfaces and abstract classes", "MEDIUM"),
                question(4, "Explain polymorphism", "MEDIUM"),
                question(5, "Apply Strategy to AIVES", "HARD"),
                question(6, "Evaluate a modular architecture", "HARD"));
        var previous = Set.of(questions.getFirst().getId());
        var usage = Map.of(
                questions.get(0).getId(), 0,
                questions.get(1).getId(), 20,
                questions.get(2).getId(), 1,
                questions.get(3).getId(), 10,
                questions.get(4).getId(), 1,
                questions.get(5).getId(), 15);

        System.out.println("=== AIVES: STRATEGY PATTERN DEMO ===");
        System.out.println("Input: 6 approved questions; select 2. Q1 appeared in the previous attempt.");
        for (var q : questions) print(q, usage);

        // The same context and input, but a different algorithm selected at runtime.
        for (String mode : List.of("RANDOM", "ADAPTIVE")) {
            System.out.println("\nMode = " + mode);
            var selected = selector.select(questions, 2, mode, previous, usage);
            selected.forEach(q -> print(q, usage));
            if (selected.size() != 2 || selected.stream().anyMatch(q -> previous.contains(q.getId())))
                throw new IllegalStateException("Selection contract violated");
            if (mode.equals("ADAPTIVE") && !Set.copyOf(selected).equals(
                    Set.of(questions.get(2), questions.get(4))))
                throw new IllegalStateException("Expected the two least-used eligible questions");
        }
        System.out.println("\nPASS: both modes avoid Q1; ADAPTIVE selects Q3 and Q5 (usage=1).");
        System.out.println("ADAPTIVE balances exposure/difficulty; it does not infer student ability.");
    }

    private static QuestionInfo question(int number, String content, String difficulty) {
        return new QuestionInfo(new UUID(0, number), null,
                "Q" + number + ": " + content, null, difficulty, "APPROVED");
    }

    private static void print(QuestionInfo question, Map<UUID, Integer> usage) {
        System.out.printf("  %-50s difficulty=%-6s usage=%d%n",
                question.getContent(), question.getDifficulty(), usage.get(question.getId()));
    }
}
