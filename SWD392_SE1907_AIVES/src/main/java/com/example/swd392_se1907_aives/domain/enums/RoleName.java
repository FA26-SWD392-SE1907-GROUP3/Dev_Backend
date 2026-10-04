package com.example.swd392_se1907_aives.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RoleName {
    ADMIN,
    LECTURER,
    STUDENT;

    @JsonCreator
    public static RoleName fromString(String value) {
        return RoleName.valueOf(value.toUpperCase());
    }
}