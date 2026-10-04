package com.example.swd392_se1907_aives.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubjectRequest(
        @NotBlank @Size(max = 20) String subjectCode,
        @NotBlank @Size(max = 100) String subjectName,
        String description) {
}
