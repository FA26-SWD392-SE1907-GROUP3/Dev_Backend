package com.example.swd392_se1907_aives;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.AiInterviewRequest;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.AttemptQuestion;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.Exam;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamAttempt;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamCandidate;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamQuestionPool;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.InterviewTurn;
import com.example.swd392_se1907_aives.modules.identity.internal.domain.entity.User;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Course;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseEnrollment;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseLecturer;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseMaterial;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseTopic;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.MaterialChunk;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Question;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionGenerationJob;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionGenerationMaterial;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionImportBatch;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionReview;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionSourceChunk;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Rubric;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.RubricCriterion;


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
