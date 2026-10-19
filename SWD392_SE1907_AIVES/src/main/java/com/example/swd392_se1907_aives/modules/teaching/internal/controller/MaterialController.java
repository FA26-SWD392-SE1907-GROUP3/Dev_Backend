package com.example.swd392_se1907_aives.modules.teaching.internal.controller;

import com.example.swd392_se1907_aives.modules.teaching.internal.domain.entity.CourseMaterial;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.MaterialService;


import lombok.RequiredArgsConstructor;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('LECTURER')")
public class MaterialController {
    private final MaterialService service;
    public record MaterialView(UUID id,UUID courseId,String originalFilename,String mimeType,String extractionStatus,OffsetDateTime uploadedAt) {}
    public record ChunkView(UUID id,int chunkIndex,String content,Integer pageNumber) {}
    public record Rename(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String name){}
    @PutMapping("/materials/{id}") public MaterialView rename(@PathVariable("id") UUID id,@jakarta.validation.Valid @RequestBody Rename req){return view(service.rename(id,req.name()));}
    @DeleteMapping("/materials/{id}") public void remove(@PathVariable("id") UUID id){service.deleteUnused(id);}
    private MaterialView view(CourseMaterial m){return new MaterialView(m.getId(),m.getCourseId(),m.getOriginalFilename(),m.getMimeType(),m.getExtractionStatus(),m.getUploadedAt());}
    @GetMapping("/courses/{courseId}/materials")
    public List<MaterialView> list(@PathVariable UUID courseId){return service.list(courseId).stream().map(this::view).toList();}
    @PostMapping(value="/courses/{courseId}/materials",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialView upload(@PathVariable UUID courseId,@RequestParam("file") MultipartFile file){return view(service.upload(courseId,file));}
    @PostMapping("/materials/{id}/index")
    public MaterialView index(@PathVariable UUID id){return view(service.index(id));}
    @GetMapping("/materials/{id}/chunks")
    public List<ChunkView> chunks(@PathVariable UUID id){return service.chunks(id).stream().map(c->new ChunkView(c.getId(),c.getChunkIndex(),c.getContent(),c.getPageNumber())).toList();}
    @GetMapping("/materials/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable UUID id){
        CourseMaterial m=service.get(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(m.getMimeType()))
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(m.getOriginalFilename(),StandardCharsets.UTF_8).build().toString())
            .header("X-Content-Type-Options","nosniff").body(new FileSystemResource(service.file(m)));
    }
}
