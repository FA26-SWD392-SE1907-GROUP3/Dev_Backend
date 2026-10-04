package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.Rubric;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RubricRepository extends JpaRepository<Rubric, Integer> { List<Rubric> findByQuestionQuestionId(Integer questionId); }
