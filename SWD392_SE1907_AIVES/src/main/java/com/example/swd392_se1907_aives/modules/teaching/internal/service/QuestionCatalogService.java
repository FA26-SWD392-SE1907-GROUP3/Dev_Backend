package com.example.swd392_se1907_aives.modules.teaching.internal.service;
import com.example.swd392_se1907_aives.modules.teaching.api.QuestionCatalog;
import com.example.swd392_se1907_aives.modules.teaching.api.QuestionInfo;
import com.example.swd392_se1907_aives.modules.teaching.api.RubricInfo;
import com.example.swd392_se1907_aives.modules.teaching.api.CriterionInfo;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.RubricRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.RubricCriterionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class QuestionCatalogService implements QuestionCatalog {
    private final QuestionRepository questions;
    private final RubricRepository rubrics;
    private final RubricCriterionRepository criteria;
    @Override public Optional<QuestionInfo> findById(UUID id) { return questions.findById(id).map(q->new QuestionInfo(q.getId(),q.getCourseId(),q.getRubricId(),q.getContent(),q.getReferenceAnswer(),q.getBloomLevel(),q.getDifficulty(),q.getStatus())); }
    @Override public Optional<RubricInfo> findRubricById(UUID id) { return rubrics.findById(id).map(r->new RubricInfo(r.getId(),r.getName(),r.getMaxScore())); }
    @Override public List<CriterionInfo> criteria(UUID rubricId) { return criteria.findByRubricId(rubricId).stream().map(c->new CriterionInfo(c.getCriterionName(),c.getDescription(),c.getMaxPoints(),c.getSortOrder())).toList(); }
}
