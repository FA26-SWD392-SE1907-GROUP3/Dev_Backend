package com.example.swd392_se1907_aives.modules.teaching.api;
import java.util.UUID;

/** Immutable data exchanged between business modules; never exposes persistence entities. */
public record QuestionInfo(UUID id, UUID courseId, UUID rubricId, String content, String referenceAnswer, String bloomLevel, String difficulty, String status) {
    public UUID getId() { return id; }
    public UUID getCourseId() { return courseId; }
    public UUID getRubricId() { return rubricId; }
    public String getContent() { return content; }
    public String getReferenceAnswer() { return referenceAnswer; }
    public String getBloomLevel() { return bloomLevel; }
    public String getDifficulty() { return difficulty; }
    public String getStatus() { return status; }
}
