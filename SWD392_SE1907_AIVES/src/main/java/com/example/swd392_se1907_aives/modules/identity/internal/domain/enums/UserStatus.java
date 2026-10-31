package com.example.swd392_se1907_aives.modules.identity.internal.domain.enums;



import com.fasterxml.jackson.annotation.JsonCreator;

public enum UserStatus {

    ACTIVE,
    INACTIVE;

    @JsonCreator
    public static UserStatus fromString(String value) {
        return UserStatus.valueOf(value.toUpperCase());
    }
}
