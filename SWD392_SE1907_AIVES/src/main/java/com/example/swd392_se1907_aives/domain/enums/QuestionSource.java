package com.example.swd392_se1907_aives.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum QuestionSource {
    MANUAL,
    AI_RAG;

    @JsonCreator
    public static QuestionSource fromString(String value) {
        return QuestionSource.valueOf(value.toUpperCase());
    }
}