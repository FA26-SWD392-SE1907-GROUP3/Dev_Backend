package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QuestionGenerationMaterialRepository extends JpaRepository<QuestionGenerationMaterial,QuestionGenerationMaterialId> {
    boolean existsByIdMaterialId(java.util.UUID materialId);
}
