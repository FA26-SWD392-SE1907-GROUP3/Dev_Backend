package com.example.swd392_se1907_aives.modules.teaching.internal.repository;

import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionSourceChunk;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionSourceChunkId;

import org.springframework.data.jpa.repository.JpaRepository;
public interface QuestionSourceChunkRepository extends JpaRepository<QuestionSourceChunk,QuestionSourceChunkId> {
    java.util.List<QuestionSourceChunk> findByIdQuestionId(java.util.UUID questionId);
}
