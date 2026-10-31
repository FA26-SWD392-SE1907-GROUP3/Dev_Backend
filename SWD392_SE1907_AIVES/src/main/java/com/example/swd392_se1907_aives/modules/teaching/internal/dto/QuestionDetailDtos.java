package com.example.swd392_se1907_aives.modules.teaching.internal.dto;

import java.util.List;
import java.util.UUID;

public final class QuestionDetailDtos {
    private QuestionDetailDtos() {}

    public record SourceView(UUID chunkId, String filename, String content, Integer pageNumber) {}

    public record DetailView(
            UUID id,
            String content,
            String referenceAnswer,
            String sourceType,
            String difficulty,
            List<SourceView> sources) {}
}
