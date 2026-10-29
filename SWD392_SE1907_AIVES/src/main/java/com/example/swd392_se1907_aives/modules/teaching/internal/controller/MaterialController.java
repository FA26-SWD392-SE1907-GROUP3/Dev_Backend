package com.example.swd392_se1907_aives.modules.teaching.internal.controller;

import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseMaterial;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.MaterialDtos.ChunkView;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.MaterialDtos.MaterialView;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.MaterialDtos.Rename;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.MaterialService;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('LECTURER')")
public class MaterialController {
    private final MaterialService service;

    @PutMapping("/materials/{id}")
    public MaterialView rename(
            @PathVariable("id") UUID id, @jakarta.validation.Valid @RequestBody Rename request) {
        return view(service.rename(id, request.name()));
    }

    @DeleteMapping("/materials/{id}")
    public void remove(@PathVariable("id") UUID id) {
        service.deleteUnused(id);
    }

    private MaterialView view(CourseMaterial m) {
        return new MaterialView(
                m.getId(),
                m.getCourseId(),
                m.getOriginalFilename(),
                m.getMimeType(),
                m.getExtractionStatus(),
                m.getUploadedAt());
    }

    @GetMapping("/courses/{courseId}/materials")
    public List<MaterialView> list(@PathVariable UUID courseId) {
        return service.list(courseId).stream().map(this::view).toList();
    }

    @PostMapping(
            value = "/courses/{courseId}/materials",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialView upload(
            @PathVariable UUID courseId, @RequestParam("file") MultipartFile file) {
        return view(service.upload(courseId, file));
    }

    @PostMapping("/materials/{id}/index")
    public MaterialView index(@PathVariable UUID id) {
        return view(service.index(id));
    }

    @GetMapping("/materials/{id}/chunks")
    public List<ChunkView> chunks(@PathVariable UUID id) {
        return service.chunks(id).stream()
                .map(
                        c ->
                                new ChunkView(
                                        c.getId(),
                                        c.getChunkIndex(),
                                        c.getContent(),
                                        c.getPageNumber()))
                .toList();
    }

    @GetMapping("/materials/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id) {
        CourseMaterial m = service.get(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(m.getMimeType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(m.getOriginalFilename(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(service.file(m)));
    }
}
