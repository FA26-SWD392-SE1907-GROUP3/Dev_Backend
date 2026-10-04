package com.example.swd392_se1907_aives.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum QuestionType {
    MAIN,
    FOLLOW_UP;

    @JsonCreator
    public static QuestionType fromString(String value) {
        return QuestionType.valueOf(value.toUpperCase());
    }
}