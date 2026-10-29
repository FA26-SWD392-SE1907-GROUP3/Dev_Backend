package com.example.swd392_se1907_aives.modules.teaching.internal.service.impl;

import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Rubric;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.RubricCriterion;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.RubricCriterionDtos.CriterionRequest;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.RubricCriterionDtos.CriterionView;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.RubricCriterionRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.RubricRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.RubricCriterionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RubricCriterionServiceImpl implements RubricCriterionService {
    private final RubricRepository rubrics;
    private final RubricCriterionRepository criteria;
    private final CourseAccessService access;

    private CriterionView criterionView(RubricCriterion c) {
        return new CriterionView(
                c.getId(),
                c.getRubricId(),
                c.getCriterionName(),
                c.getDescription(),
                c.getMaxPoints(),
                c.getSortOrder());
    }

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    public List<CriterionView> criteria(UUID id) {
        Rubric r =
                rubrics.findById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Rubric not found"));
        access.requireTeacher(r.getCourseId());
        return criteria.findByRubricId(id).stream()
                .sorted(Comparator.comparing(RubricCriterion::getSortOrder))
                .map(this::criterionView)
                .toList();
    }

    private Rubric editableRubric(UUID id) {
        Rubric r =
                rubrics.findLockedById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Rubric not found"));
        access.requireTeacher(r.getCourseId());
        if (!"DRAFT".equals(r.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rubric is not a draft");
        return r;
    }

    private void validateCriterion(Rubric rubric, CriterionRequest request, UUID excludedId) {
        var others =
                criteria.findByRubricId(rubric.getId()).stream()
                        .filter(c -> !c.getId().equals(excludedId))
                        .toList();
        if (others.stream().anyMatch(c -> c.getSortOrder() == request.sortOrder()))
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Thứ tự tiêu chí đã được sử dụng.");
        BigDecimal total =
                others.stream()
                        .map(RubricCriterion::getMaxPoints)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.add(request.maxPoints()).compareTo(rubric.getMaxScore()) > 0)
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Tổng điểm tiêu chí vượt điểm tối đa của rubric.");
    }

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public CriterionView addCriterion(UUID id, CriterionRequest request) {
        Rubric rubric = editableRubric(id);
        validateCriterion(rubric, request, null);
        RubricCriterion c = new RubricCriterion();
        c.setRubricId(id);
        c.setCriterionName(request.criterionName());
        c.setDescription(request.description());
        c.setMaxPoints(request.maxPoints());
        c.setSortOrder(request.sortOrder());
        c = criteria.save(c);
        return criterionView(c);
    }

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public CriterionView updateCriterion(UUID id, UUID criterionId, CriterionRequest request) {
        Rubric r = editableRubric(id);
        RubricCriterion c =
                criteria.findById(criterionId)
                        .filter(x -> id.equals(x.getRubricId()))
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Criterion not found"));
        validateCriterion(r, request, criterionId);
        c.setCriterionName(request.criterionName());
        c.setDescription(request.description());
        c.setMaxPoints(request.maxPoints());
        c.setSortOrder(request.sortOrder());
        return criterionView(criteria.save(c));
    }

    @Override
    @PreAuthorize("hasRole('LECTURER')")
    @Transactional
    public void deleteCriterion(UUID id, UUID criterionId) {
        editableRubric(id);
        RubricCriterion c =
                criteria.findById(criterionId)
                        .filter(x -> id.equals(x.getRubricId()))
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Criterion not found"));
        criteria.delete(c);
    }
}
