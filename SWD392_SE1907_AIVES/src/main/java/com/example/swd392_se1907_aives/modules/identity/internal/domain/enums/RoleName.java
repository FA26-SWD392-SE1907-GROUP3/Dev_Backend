package com.example.swd392_se1907_aives.modules.identity.internal.domain.enums;



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
