package com.example.swd392_se1907_aives.modules.teaching.internal.repository;

import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Question;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface QuestionRepository extends JpaRepository<Question, UUID> {
    boolean existsByRubricId(UUID rubricId);
    java.util.List<Question> findByCourseId(UUID courseId);
    java.util.List<Question> findByGenerationJobId(UUID generationJobId);
    @org.springframework.data.jpa.repository.Query("select q.courseId from Question q where q.id = :id")
    java.util.Optional<UUID> findCourseIdById(@org.springframework.data.repository.query.Param("id") UUID id);


}
