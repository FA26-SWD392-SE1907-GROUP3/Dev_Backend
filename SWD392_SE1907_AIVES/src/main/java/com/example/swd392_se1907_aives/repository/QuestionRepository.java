package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface QuestionRepository extends JpaRepository<Question, Integer> { List<Question> findBySubjectSubjectId(Integer subjectId); List<Question> findBySubjectSubjectIdAndStatus(Integer subjectId, com.example.swd392_se1907_aives.domain.enums.QuestionStatus status); }
