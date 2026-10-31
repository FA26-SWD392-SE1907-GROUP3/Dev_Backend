package com.example.swd392_se1907_aives.modules.identity.api;
import java.util.UUID;
import java.time.OffsetDateTime;

/** Immutable data exchanged between business modules; never exposes persistence entities. */
public record Account(UUID id, String fullName, String email, String role, Boolean isActive, OffsetDateTime createdAt) {
    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Boolean getIsActive() { return isActive; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
