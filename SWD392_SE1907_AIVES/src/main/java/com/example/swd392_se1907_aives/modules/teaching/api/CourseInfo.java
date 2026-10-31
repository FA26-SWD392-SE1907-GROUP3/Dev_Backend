package com.example.swd392_se1907_aives.modules.teaching.api;
import java.util.UUID;

/** Immutable data exchanged between business modules; never exposes persistence entities. */
public record CourseInfo(UUID id, String code, String name, String description, Boolean isActive) {
    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Boolean getIsActive() { return isActive; }
}
