package com.example.swd392_se1907_aives.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ExamStatus {
    DRAFT,
    PUBLISHED,
    COMPLETED;

    @JsonCreator
    public static ExamStatus fromString(String value) {
        return ExamStatus.valueOf(value.toUpperCase());
    }
}