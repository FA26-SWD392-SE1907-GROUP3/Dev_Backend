package com.example.swd392_se1907_aives.dto;

import com.example.swd392_se1907_aives.domain.entity.Subject;

public record SubjectResponse(Integer subjectId, String subjectCode, String subjectName, String description) {
    public static SubjectResponse from(Subject subject) {
        return new SubjectResponse(subject.getSubjectId(), subject.getSubjectCode(),
                subject.getSubjectName(), subject.getDescription());
    }
}
