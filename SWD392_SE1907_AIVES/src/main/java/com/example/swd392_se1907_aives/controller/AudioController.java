package com.example.swd392_se1907_aives.controller;
import com.example.swd392_se1907_aives.service.AudioService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import lombok.RequiredArgsConstructor;
@RestController
@RequestMapping("/api/logs/{id}/audio")
@RequiredArgsConstructor
public class AudioController {
 private final AudioService service;
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasRole('STUDENT')") public void upload(@PathVariable Integer id,@RequestParam MultipartFile file) { service.upload(id,file); }
 @GetMapping public ResponseEntity<FileSystemResource> download(@PathVariable Integer id) {
  var file=service.file(id); String ext=file.getFileName().toString().replaceAll(".*\\.","");
  String type=switch(ext) { case "wav" -> "audio/wav"; case "ogg" -> "audio/ogg"; case "m4a" -> "audio/mp4"; default -> "audio/webm"; };
  return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).body(new FileSystemResource(file));
 }
}
