package com.example.swd392_se1907_aives.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum QuestionStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED;

    @JsonCreator
    public static QuestionStatus fromString(String value) {
        return QuestionStatus.valueOf(value.toUpperCase());
    }
}