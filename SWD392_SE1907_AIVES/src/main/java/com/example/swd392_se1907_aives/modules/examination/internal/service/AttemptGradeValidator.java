package com.example.swd392_se1907_aives.modules.examination.internal.service;

import com.example.swd392_se1907_aives.modules.examination.internal.dto.AttemptGradingDtos.CriterionScore;
import com.example.swd392_se1907_aives.modules.examination.internal.dto.AttemptGradingDtos.GradingResult;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Component
public class AttemptGradeValidator {
    public GradingResult validate(JsonNode rubric, JsonNode response) {
        Map<Integer, JsonNode> expected = new HashMap<>();
        BigDecimal maxScore = number(rubric.get("maxScore"));
        BigDecimal available = BigDecimal.ZERO;
        for (var criterion : rubric.path("criteria")) {
            int order = criterion.path("sortOrder").asInt(-1);
            if (order < 1 || expected.put(order, criterion) != null) throw invalid();
            BigDecimal maximum = number(criterion.get("maxPoints"));
            if (maximum.signum() <= 0) throw invalid();
            available = available.add(maximum);
        }
        if (expected.isEmpty() || maxScore.signum() <= 0 || available.compareTo(maxScore) != 0)
            throw invalid();
        if (!response.path("criteria").isArray()
                || response.path("criteria").size() != expected.size()) throw invalid();
        var seen = new HashSet<Integer>();
        var scores = new ArrayList<CriterionScore>();
        BigDecimal total = BigDecimal.ZERO;
        for (var item : response.path("criteria")) {
            int order = item.path("sortOrder").asInt(-1);
            JsonNode criterion = expected.get(order);
            if (criterion == null || !seen.add(order)) throw invalid();
            BigDecimal score = number(item.get("score"));
            BigDecimal maximum = number(criterion.get("maxPoints"));
            if (score.signum() < 0
                    || score.compareTo(maximum) > 0
                    || score.stripTrailingZeros().scale() > 2) throw invalid();
            scores.add(
                    new CriterionScore(
                            order,
                            criterion.path("name").asText(),
                            score,
                            maximum,
                            feedback(item)));
            total = total.add(score);
        }
        scores.sort(java.util.Comparator.comparingInt(CriterionScore::sortOrder));
        return new GradingResult(
                total, maxScore, feedback(response), java.util.List.copyOf(scores));
    }

    private BigDecimal number(JsonNode value) {
        if (value == null || !value.isNumber()) throw invalid();
        return value.decimalValue();
    }

    private String feedback(JsonNode value) {
        if (!value.path("feedback").isTextual()
                || value.path("feedback").asText().isBlank()
                || value.path("feedback").asText().length() > 12000) throw invalid();
        return value.path("feedback").asText();
    }

    private IllegalArgumentException invalid() {
        return new IllegalArgumentException("Kết quả AI không khớp rubric chấm toàn bài.");
    }
}
