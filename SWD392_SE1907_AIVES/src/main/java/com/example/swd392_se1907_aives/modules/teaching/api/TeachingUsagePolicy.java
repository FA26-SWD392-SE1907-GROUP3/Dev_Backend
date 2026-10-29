package com.example.swd392_se1907_aives.modules.teaching.api;
import java.util.UUID;

/** Extension point for consumers of teaching content to protect their examination history. */
public interface TeachingUsagePolicy {
    boolean hasRubricInUse(UUID rubricId);
    boolean hasOpenExams(UUID courseId);
    boolean hasStudentInProgress(UUID courseId,UUID studentId);
    /** Called in the same transaction after Teaching has locked the course. */
    void prepareQuestionChange(UUID questionId);
}
