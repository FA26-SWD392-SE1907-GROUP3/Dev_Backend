package com.example.swd392_se1907_aives.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ScheduleStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    MISSED;

    @JsonCreator
    public static ScheduleStatus fromString(String value) {
        return ScheduleStatus.valueOf(value.toUpperCase());
    }
}