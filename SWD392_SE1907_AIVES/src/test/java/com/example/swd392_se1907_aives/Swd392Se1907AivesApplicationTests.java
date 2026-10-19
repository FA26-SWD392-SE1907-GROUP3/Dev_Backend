package com.example.swd392_se1907_aives;

import com.example.swd392_se1907_aives.domain.entity.*;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class Swd392Se1907AivesApplicationTests {
    @Test
    void mappingsCoverAll22AivesTables() throws NoSuchFieldException {
        Class<?>[] entities = {
            User.class, Course.class, CourseLecturer.class, CourseEnrollment.class,
            CourseTopic.class, CourseMaterial.class, MaterialChunk.class,
            QuestionGenerationJob.class, QuestionGenerationMaterial.class, Rubric.class,
            RubricCriterion.class, QuestionImportBatch.class, Question.class,
            QuestionReview.class, QuestionSourceChunk.class, Exam.class,
            ExamQuestionPool.class, ExamCandidate.class, ExamAttempt.class,
            AttemptQuestion.class, InterviewTurn.class, AiInterviewRequest.class
        };
        Set<String> tableNames = new HashSet<>();
        for (Class<?> entity : entities) {
            assertNotNull(entity.getAnnotation(Table.class), "Missing @Table: " + entity.getName());
            tableNames.add(entity.getAnnotation(Table.class).name());
        }
        assertEquals(22, tableNames.size());
        assertEquals(UUID.class, User.class.getDeclaredField("id").getType());
    }
}
