package com.example.swd392_se1907_aives.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BloomLevel {
    REMEMBER,    // Nhớ
    UNDERSTAND,  // Hiểu
    APPLY,       // Vận dụng
    ANALYZE;     // Phân tích

    @JsonCreator
    public static BloomLevel fromString(String value) {
        return BloomLevel.valueOf(value.toUpperCase());
    }
}