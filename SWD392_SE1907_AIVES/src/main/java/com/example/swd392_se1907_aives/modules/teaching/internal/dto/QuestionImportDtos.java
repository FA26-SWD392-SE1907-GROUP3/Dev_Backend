package com.example.swd392_se1907_aives.modules.teaching.internal.dto;

import java.util.List;
import java.util.UUID;

public final class QuestionImportDtos {
    private QuestionImportDtos() {}

    public record PreviewRow(
            int row,
            String content,
            String referenceAnswer,
            String difficulty,
            List<String> errors) {}

    public record Result(
            UUID batchId, String status, int totalRows, int importedRows, List<PreviewRow> rows) {}
}
