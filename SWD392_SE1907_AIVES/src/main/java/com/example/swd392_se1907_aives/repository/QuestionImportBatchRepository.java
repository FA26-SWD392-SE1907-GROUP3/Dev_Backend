package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.QuestionImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface QuestionImportBatchRepository extends JpaRepository<QuestionImportBatch,UUID>{
    List<QuestionImportBatch> findByCourseIdOrderByImportedAtDesc(UUID courseId);
}
