package com.example.swd392_se1907_aives.modules.teaching.internal.repository;

import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionGenerationMaterial;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionGenerationMaterialId;

import org.springframework.data.jpa.repository.JpaRepository;
public interface QuestionGenerationMaterialRepository extends JpaRepository<QuestionGenerationMaterial,QuestionGenerationMaterialId> {
    boolean existsByIdMaterialId(java.util.UUID materialId);
}
