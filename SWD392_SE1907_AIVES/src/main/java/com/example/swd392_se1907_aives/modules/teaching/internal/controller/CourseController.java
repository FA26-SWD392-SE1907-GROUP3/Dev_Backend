package com.example.swd392_se1907_aives.modules.teaching.internal.controller;

import com.example.swd392_se1907_aives.modules.teaching.internal.dto.CourseDtos.CourseView;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.CourseDtos.CreateCourse;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.CourseDtos.LecturerView;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.CourseDtos.StudentView;
import com.example.swd392_se1907_aives.modules.teaching.internal.dto.CourseDtos.UserIdRequest;
import com.example.swd392_se1907_aives.modules.teaching.internal.service.CourseService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
public class CourseController {
    private final CourseService service;

    @GetMapping
    public List<CourseView> list(@RequestParam(required = false) UUID lecturerId) {
        return service.list(lecturerId);
    }

    @GetMapping("/{courseId}/students")
    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    public List<StudentView> students(@PathVariable UUID courseId) {
        return service.students(courseId);
    }

    @GetMapping("/{courseId}/lecturers")
    @PreAuthorize("hasAnyRole('ADMIN','LECTURER')")
    public List<LecturerView> assignedLecturers(@PathVariable UUID courseId) {
        return service.assignedLecturers(courseId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CourseView create(@Valid @RequestBody CreateCourse request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseView update(
            @PathVariable("id") UUID id, @Valid @RequestBody CreateCourse request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void remove(@PathVariable("id") UUID id) {
        service.remove(id);
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseView restore(@PathVariable("id") UUID id) {
        return service.restore(id);
    }

    @DeleteMapping("/{courseId}/lecturers/{lecturerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void unassign(
            @PathVariable("courseId") UUID courseId, @PathVariable("lecturerId") UUID lecturerId) {
        service.unassign(courseId, lecturerId);
    }

    @PutMapping("/{courseId}/lecturers/{lecturerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void replaceLecturer(
            @PathVariable("courseId") UUID courseId,
            @PathVariable("lecturerId") UUID lecturerId,
            @Valid @RequestBody UserIdRequest request) {
        service.replaceLecturer(courseId, lecturerId, request);
    }

    @DeleteMapping("/{courseId}/students/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void unenroll(
            @PathVariable("courseId") UUID courseId, @PathVariable("studentId") UUID studentId) {
        service.unenroll(courseId, studentId);
    }

    @PostMapping("/{courseId}/lecturers")
    @PreAuthorize("hasRole('ADMIN')")
    public void assignLecturer(
            @PathVariable UUID courseId, @Valid @RequestBody UserIdRequest request) {
        service.assignLecturer(courseId, request);
    }

    @PostMapping("/{courseId}/students")
    @PreAuthorize("hasRole('ADMIN')")
    public void enroll(@PathVariable UUID courseId, @Valid @RequestBody UserIdRequest request) {
        service.enroll(courseId, request);
    }
}
