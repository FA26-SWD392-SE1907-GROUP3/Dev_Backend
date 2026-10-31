package com.example.swd392_se1907_aives.modules.teaching.api;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-only question/rubric snapshots. Approval and editing remain owned by Teaching. */
public interface QuestionCatalog {
    Optional<QuestionInfo> findById(UUID id);
    Optional<RubricInfo> findRubricById(UUID id);
    List<CriterionInfo> criteria(UUID rubricId);
}
