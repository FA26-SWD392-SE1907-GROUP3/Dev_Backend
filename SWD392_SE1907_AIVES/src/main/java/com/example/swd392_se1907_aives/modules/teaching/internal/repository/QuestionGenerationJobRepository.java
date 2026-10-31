package com.example.swd392_se1907_aives.modules.teaching.internal.repository;

import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionGenerationJob;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface QuestionGenerationJobRepository extends JpaRepository<QuestionGenerationJob,UUID> {
    List<QuestionGenerationJob> findByCourseIdOrderByCreatedAtDesc(UUID courseId);
}
