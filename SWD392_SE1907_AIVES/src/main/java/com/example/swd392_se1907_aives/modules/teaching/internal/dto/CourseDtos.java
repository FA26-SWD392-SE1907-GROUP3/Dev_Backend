package com.example.swd392_se1907_aives.modules.teaching.internal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class CourseDtos {
    private CourseDtos() {}

    public record CreateCourse(
            @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description) {}

    public record CourseView(
            UUID id, String code, String name, String description, boolean active) {}

    public record LecturerView(UUID id, String fullName, String email) {}

    public record StudentView(
            UUID id, String fullName, String email, String enrollmentStatus, boolean active) {}

    public record UserIdRequest(@NotBlank @Email String email) {}
}
