package com.example.swd392_se1907_aives.modules.identity.internal.domain.dto.request;

import com.example.swd392_se1907_aives.modules.identity.internal.domain.enums.RoleName;
import jakarta.validation.constraints.NotNull;

public record RegistrationApprovalRequest(@NotNull(message="Vui lòng chọn vai trò") RoleName role) {}
