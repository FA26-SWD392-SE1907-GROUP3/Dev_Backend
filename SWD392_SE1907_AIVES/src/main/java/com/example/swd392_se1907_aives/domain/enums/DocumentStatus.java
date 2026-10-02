package com.example.swd392_se1907_aives.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DocumentStatus {
    PROCESSING,
    INDEXED,
    ERROR;

    @JsonCreator
    public static DocumentStatus fromString(String value) {
        return DocumentStatus.valueOf(value.toUpperCase());
    }
}