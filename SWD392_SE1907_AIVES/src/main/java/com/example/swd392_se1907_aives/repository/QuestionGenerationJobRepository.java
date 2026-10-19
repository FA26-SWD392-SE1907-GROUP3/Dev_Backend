package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.QuestionGenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface QuestionGenerationJobRepository extends JpaRepository<QuestionGenerationJob,UUID> {
    List<QuestionGenerationJob> findByCourseIdOrderByCreatedAtDesc(UUID courseId);
}
