package com.example.swd392_se1907_aives.modules.teaching.api;
import java.util.UUID;
public record QuestionInfo(UUID id,UUID courseId,String content,String referenceAnswer,String difficulty,String status) {
    public UUID getId() {return id;}
    public UUID getCourseId() {return courseId;}
    public String getContent() {return content;}
    public String getReferenceAnswer() {return referenceAnswer;}
    public String getDifficulty() {return difficulty;}
    public String getStatus() {return status;}
}
