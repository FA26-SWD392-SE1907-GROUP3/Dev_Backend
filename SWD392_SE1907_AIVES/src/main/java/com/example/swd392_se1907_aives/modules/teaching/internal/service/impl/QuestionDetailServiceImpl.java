package com.example.swd392_se1907_aives.modules.teaching.internal.service.impl;

import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionDetailDtos.DetailView;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionDetailDtos.SourceView;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseMaterialRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.MaterialChunkRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionSourceChunkRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.QuestionDetailService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('LECTURER')")
public class QuestionDetailServiceImpl implements QuestionDetailService {
    private final QuestionRepository questions;
    private final QuestionSourceChunkRepository sources;
    private final MaterialChunkRepository chunks;
    private final CourseMaterialRepository materials;
    private final CourseAccessService access;

    @Override
    public DetailView detail(UUID id) {
        var q =
                questions
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Question not found"));
        access.requireTeacher(q.getCourseId());
        var references =
                sources.findByIdQuestionId(id).stream()
                        .map(
                                link -> {
                                    var chunk =
                                            chunks.findById(link.getId().getChunkId())
                                                    .orElseThrow();
                                    var material =
                                            materials.findById(chunk.getMaterialId()).orElseThrow();
                                    if (!q.getCourseId().equals(material.getCourseId()))
                                        throw new ResponseStatusException(
                                                HttpStatus.CONFLICT, "Nguồn câu hỏi không hợp lệ.");
                                    return new SourceView(
                                            chunk.getId(),
                                            material.getOriginalFilename(),
                                            chunk.getContent(),
                                            chunk.getPageNumber());
                                })
                        .toList();
        return new DetailView(
                q.getId(),
                q.getContent(),
                q.getReferenceAnswer(),
                q.getSourceType(),
                q.getDifficulty(),
                references);
    }
}
