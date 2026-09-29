package com.clouddocs.tenancy.api;

import com.clouddocs.tenancy.domain.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull Role role) {
}
