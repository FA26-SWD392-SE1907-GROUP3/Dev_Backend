package com.example.swd392_se1907_aives.modules.teaching.internal.controller;

import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.RubricCriterion;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseMaterialRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.MaterialChunkRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionSourceChunkRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.RubricCriterionRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.RubricRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/questions")
@PreAuthorize("hasRole('LECTURER')")
public class QuestionDetailController {
    private final QuestionRepository questions;
    private final QuestionSourceChunkRepository sources;
    private final MaterialChunkRepository chunks;
    private final CourseMaterialRepository materials;
    private final CourseAccessService access;
    private final RubricRepository rubrics;
    private final RubricCriterionRepository criteria;
    public record SourceView(UUID chunkId,String filename,String content,Integer pageNumber) {}
    public record CriterionView(String name,String description,java.math.BigDecimal maxPoints){}
    public record RubricView(UUID id,String name,java.math.BigDecimal maxScore,String status,List<CriterionView> criteria){}
    public record DetailView(UUID id,String content,String referenceAnswer,String sourceType,String bloomLevel,String difficulty,RubricView rubric,List<SourceView> sources) {}
    @GetMapping("/{id}") public DetailView detail(@PathVariable UUID id){
        var q=questions.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Question not found"));
        access.requireTeacher(q.getCourseId());
        var references=sources.findByIdQuestionId(id).stream().map(link->{
            var chunk=chunks.findById(link.getId().getChunkId()).orElseThrow();
            var material=materials.findById(chunk.getMaterialId()).orElseThrow();
            if(!q.getCourseId().equals(material.getCourseId()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Nguồn câu hỏi không hợp lệ.");
            return new SourceView(chunk.getId(),material.getOriginalFilename(),chunk.getContent(),chunk.getPageNumber());
        }).toList();
        RubricView rubric=q.getRubricId()==null?null:rubrics.findById(q.getRubricId()).filter(r->q.getCourseId().equals(r.getCourseId())).map(r->new RubricView(r.getId(),r.getName(),r.getMaxScore(),r.getStatus(),
            criteria.findByRubricId(r.getId()).stream().sorted(Comparator.comparing(com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.RubricCriterion::getSortOrder)).map(c->new CriterionView(c.getCriterionName(),c.getDescription(),c.getMaxPoints())).toList())).orElse(null);
        return new DetailView(q.getId(),q.getContent(),q.getReferenceAnswer(),q.getSourceType(),q.getBloomLevel(),q.getDifficulty(),rubric,references);
    }
}
