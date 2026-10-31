package com.example.swd392_se1907_aives.modules.teaching.api;
import java.util.UUID;
import java.math.BigDecimal;
public record RubricInfo(UUID id, UUID courseId,String name,BigDecimal maxScore,String status) {
    public UUID getId() {return id;}
    public UUID getCourseId() {return courseId;}
    public String getName() {return name;}
    public BigDecimal getMaxScore() {return maxScore;}
    public String getStatus() {return status;}
}
