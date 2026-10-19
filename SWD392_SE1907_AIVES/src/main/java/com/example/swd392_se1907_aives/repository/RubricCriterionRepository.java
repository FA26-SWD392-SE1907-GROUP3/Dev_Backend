package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.RubricCriterion;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface RubricCriterionRepository extends JpaRepository<RubricCriterion, UUID> {
    java.util.List<RubricCriterion> findByRubricId(UUID rubricId);



}
