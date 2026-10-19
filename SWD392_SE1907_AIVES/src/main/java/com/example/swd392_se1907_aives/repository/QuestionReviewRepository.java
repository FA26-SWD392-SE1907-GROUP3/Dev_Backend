package com.example.swd392_se1907_aives.repository;
import com.example.swd392_se1907_aives.domain.entity.QuestionReview;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface QuestionReviewRepository extends JpaRepository<QuestionReview, UUID> {



}
