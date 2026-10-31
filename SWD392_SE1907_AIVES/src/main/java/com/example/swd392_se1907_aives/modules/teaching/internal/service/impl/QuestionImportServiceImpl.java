package com.example.swd392_se1907_aives.modules.teaching.internal.service.impl;

import com.example.swd392_se1907_aives.modules.teaching.api.CourseAccessService;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.Question;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.QuestionImportBatch;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.policy.QuestionClassification;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionImportDtos.PreviewRow;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.QuestionImportDtos.Result;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.CourseRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionImportBatchRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.repository.QuestionRepository;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.QuestionImportParser;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.QuestionImportService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionImportServiceImpl implements QuestionImportService {
    private final QuestionImportParser parser;
    private final QuestionRepository questions;
    private final CourseRepository courses;
    private final CourseAccessService access;
    private final QuestionImportBatchRepository batches;
    private final ObjectMapper json;

    @Override
    @Transactional
    public Result process(UUID courseId, String filename, byte[] bytes, boolean preview) {
        access.requireTeacher(courseId);
        courses.findLockedById(courseId)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Không tìm thấy môn học."));
        Set<String> seen = new HashSet<>();
        questions.findByCourseId(courseId).stream()
                .filter(q -> !"ARCHIVED".equals(q.getStatus()))
                .forEach(q -> seen.add(normalize(q.getContent())));
        List<PreviewRow> rows = new ArrayList<>();
        for (var row : parser.parse(filename, bytes)) {
            List<String> errors = new ArrayList<>();
            String content = row.content().strip();
            String difficulty = row.difficulty().strip().toUpperCase(Locale.ROOT);
            if (content.isBlank() || content.length() > 12000)
                errors.add("Nội dung phải từ 1 đến 12.000 ký tự.");
            if (row.referenceAnswer().length() > 24000) errors.add("Đáp án vượt 24.000 ký tự.");
            if (!QuestionClassification.isValidDifficulty(difficulty))
                errors.add("Mức câu hỏi phải là EASY (Dễ), MEDIUM (Trung bình) hoặc HARD (Khó).");
            if (!seen.add(normalize(content)))
                errors.add("Nội dung câu hỏi bị trùng trong tệp hoặc trong môn học.");
            rows.add(
                    new PreviewRow(
                            row.row(), content, row.referenceAnswer().strip(), difficulty, errors));
        }
        boolean invalid = rows.stream().anyMatch(row -> !row.errors().isEmpty());
        if (preview) return new Result(null, invalid ? "INVALID" : "READY", rows.size(), 0, rows);
        QuestionImportBatch batch = new QuestionImportBatch();
        batch.setCourseId(courseId);
        batch.setImportedBy(access.current().getId());
        batch.setOriginalFilename(
                filename.length() > 255 ? filename.substring(filename.length() - 255) : filename);
        batch.setTotalRows(rows.size());
        batch.setImportedRows(invalid ? 0 : rows.size());
        batch.setStatus(invalid ? "FAILED" : "COMPLETED");
        batch.setImportedAt(OffsetDateTime.now());
        batch.setErrorDetails(
                json.valueToTree(rows.stream().filter(row -> !row.errors().isEmpty()).toList()));
        batch = batches.saveAndFlush(batch);
        if (!invalid)
            for (var row : rows) {
                Question question = new Question();
                question.setCourseId(courseId);
                question.setCreatedBy(access.current().getId());
                question.setContent(row.content());
                question.setReferenceAnswer(row.referenceAnswer());
                question.setDifficulty(row.difficulty());
                question.setSourceType("IMPORTED");
                question.setImportBatchId(batch.getId());
                question.setStatus("PENDING_REVIEW");
                question.setCreatedAt(OffsetDateTime.now());
                question.setUpdatedAt(OffsetDateTime.now());
                questions.save(question);
            }
        return new Result(
                batch.getId(), batch.getStatus(), rows.size(), batch.getImportedRows(), rows);
    }

    private String normalize(String text) {
        return text.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
