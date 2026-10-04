package com.example.swd392_se1907_aives.controller;

import com.example.swd392_se1907_aives.dto.SubjectRequest;
import com.example.swd392_se1907_aives.dto.SubjectResponse;
import com.example.swd392_se1907_aives.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService service;

    @GetMapping
    public Page<SubjectResponse> list(@PageableDefault(size = 20, sort = "subjectId") Pageable pageable) {
        return service.findAll(pageable);
    }

    @GetMapping("/{id}")
    public SubjectResponse get(@PathVariable Integer id) {
        return service.findById(id);
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubjectResponse> create(@Valid @RequestBody SubjectRequest request) {
        SubjectResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/subjects/" + response.subjectId())).body(response);
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public SubjectResponse update(@PathVariable Integer id, @Valid @RequestBody SubjectRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
