package com.example.swd392_se1907_aives.service;

import com.example.swd392_se1907_aives.modules.examination.internal.service.QuestionSelectionStrategy;
import com.example.swd392_se1907_aives.modules.examination.internal.service.QuestionSetSelector;
import com.example.swd392_se1907_aives.modules.examination.internal.service.impl.AdaptiveQuestionSelectionImpl;
import com.example.swd392_se1907_aives.modules.examination.internal.service.impl.RandomQuestionSelectionImpl;
import com.example.swd392_se1907_aives.modules.teaching.api.QuestionInfo;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class QuestionSelectionStrategyTests {
    @Test
    void springInjectsBothStrategiesAndUsesTheirNoArgConstructors() {
        try (var spring = new AnnotationConfigApplicationContext()) {
            spring.register(QuestionSetSelector.class, RandomQuestionSelectionImpl.class,
                    AdaptiveQuestionSelectionImpl.class);
            spring.refresh();
            var selector = spring.getBean(QuestionSetSelector.class);
            var input = List.of(question());
            for (String mode : List.of("RANDOM", "ADAPTIVE"))
                assertThat(selector.select(input, 1, mode, Set.of(), Map.of())).containsExactlyElementsOf(input);
            assertThat(spring.getBeansOfType(QuestionSelectionStrategy.class)).hasSize(2);
        }
    }

    @Test
    void extensionNeedsOnlyAnotherStrategyAndRegistration() {
        var first = question();
        var second = question();
        QuestionSelectionStrategy reverse = new QuestionSelectionStrategy() {
            public String mode() { return "DEMO_REVERSE"; }
            public List<QuestionInfo> select(List<QuestionInfo> eligible, int count,
                    Set<UUID> previous, Map<UUID, Integer> usage) {
                return eligible.reversed().subList(0, count);
            }
        };
        var selector = new QuestionSetSelector(List.of(new RandomQuestionSelectionImpl(), reverse));
        assertThat(selector.select(List.of(first, second), 1, "DEMO_REVERSE", Set.of(), Map.of()))
                .containsExactly(second);
    }

    @Test
    void invalidCountsFailAndInputIsNotModified() {
        var input = List.of(question(), question());
        var selector = new QuestionSetSelector(List.of(new RandomQuestionSelectionImpl()));
        for (int count : List.of(-1, 0, 3))
            assertThatIllegalArgumentException().isThrownBy(
                    () -> selector.select(input, count, "RANDOM", Set.of(), Map.of()));
        for (String mode : List.of("RANDOM", "UNKNOWN"))
            assertThat(selector.select(input, 1, mode, Set.of(), Map.of())).hasSize(1);
        assertThat(selector.select(input, 1, null, Set.of(), Map.of())).hasSize(1);
        assertThat(input).hasSize(2);
    }

    @Test
    void presentationRunsWithRealStrategies() {
        StrategyPatternDemo.main(new String[0]);
    }

    private QuestionInfo question() {
        return new QuestionInfo(UUID.randomUUID(), null, "Demo", null, "EASY", "APPROVED");
    }
}
