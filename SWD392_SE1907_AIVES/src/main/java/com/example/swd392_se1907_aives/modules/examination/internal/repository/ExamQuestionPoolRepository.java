package com.example.swd392_se1907_aives.modules.examination.internal.repository;

import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamQuestionPool;
import com.example.swd392_se1907_aives.modules.examination.internal.domain.entity.ExamQuestionPoolId;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ExamQuestionPoolRepository extends JpaRepository<ExamQuestionPool, ExamQuestionPoolId> {
    java.util.List<ExamQuestionPool> findByIdExamId(UUID examId);
    java.util.List<ExamQuestionPool> findByIdQuestionId(UUID questionId);
}
