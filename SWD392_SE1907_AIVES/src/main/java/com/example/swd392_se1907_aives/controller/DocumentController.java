package com.example.swd392_se1907_aives.controller;
import com.example.swd392_se1907_aives.service.DocumentService;
import com.example.swd392_se1907_aives.dto.ApiModels.Generate;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.Map;
@RestController
@RequestMapping("/api/documents")
@PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
@RequiredArgsConstructor
public class DocumentController {
 private final DocumentService service;
 @GetMapping("/{id}") public Object detail(@PathVariable Integer id) { return service.detail(id); }
 @GetMapping public Object list(@RequestParam(required=false) Integer subjectId) { return service.list(subjectId); }
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Object upload(@RequestParam Integer subjectId,@RequestParam String title,@RequestParam MultipartFile file) { return service.upload(subjectId,title,file); }
 @PutMapping("/{id}") public Object rename(@PathVariable Integer id,@RequestBody Map<String,String> input) { return service.rename(id,input.get("title")); }
 @PostMapping("/{id}/reindex") public Object index(@PathVariable Integer id) { return service.reindex(id); }
 @PostMapping("/{id}/generate") public Object generate(@PathVariable Integer id,@Valid @RequestBody Generate input) { return service.generate(id,input); }
 @DeleteMapping("/{id}") public void delete(@PathVariable Integer id) { service.delete(id); }
 @GetMapping("/{id}/download") public ResponseEntity<FileSystemResource> download(@PathVariable Integer id) {
  var file=service.file(id);
  return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"document."+file.getFileName().toString().replaceAll(".*\\.","")+"\"").body(new FileSystemResource(file));
 }
}
