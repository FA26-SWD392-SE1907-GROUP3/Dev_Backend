package com.example.swd392_se1907_aives.modules.teaching.api;
import java.math.BigDecimal;

/** Immutable data exchanged between business modules; never exposes persistence entities. */
public record CriterionInfo(String criterionName, String description, BigDecimal maxPoints, Integer sortOrder) {
    public String getCriterionName() { return criterionName; }
    public String getDescription() { return description; }
    public BigDecimal getMaxPoints() { return maxPoints; }
    public Integer getSortOrder() { return sortOrder; }
}
