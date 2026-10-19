package com.example.swd392_se1907_aives.modules.teaching.api;
import java.util.UUID;
import java.math.BigDecimal;

/** Immutable data exchanged between business modules; never exposes persistence entities. */
public record RubricInfo(UUID id, String name, BigDecimal maxScore) {
    public UUID getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getMaxScore() { return maxScore; }
}
