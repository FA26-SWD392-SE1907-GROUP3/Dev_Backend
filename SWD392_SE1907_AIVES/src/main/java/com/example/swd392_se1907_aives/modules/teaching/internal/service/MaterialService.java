package com.example.swd392_se1907_aives.modules.teaching.internal.service;

import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseMaterial;
import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.MaterialChunk;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface MaterialService {
    String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    List<CourseMaterial> list(UUID courseId);

    CourseMaterial get(UUID id);

    CourseMaterial upload(UUID courseId, MultipartFile file);

    CourseMaterial index(UUID id);

    List<MaterialChunk> chunks(UUID id);

    CourseMaterial rename(UUID id, String name);

    void deleteUnused(UUID id);

    Path file(CourseMaterial material);
}
