package com.example.swd392_se1907_aives.modules.teaching.internal.repository;

import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionImportBatch;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface QuestionImportBatchRepository extends JpaRepository<QuestionImportBatch,UUID>{
    List<QuestionImportBatch> findByCourseIdOrderByImportedAtDesc(UUID courseId);
}
