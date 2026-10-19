package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QuestionSourceChunkRepository extends JpaRepository<QuestionSourceChunk,QuestionSourceChunkId> {
    java.util.List<QuestionSourceChunk> findByIdQuestionId(java.util.UUID questionId);
}
