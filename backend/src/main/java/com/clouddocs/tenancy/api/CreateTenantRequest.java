package com.clouddocs.tenancy.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(@NotBlank @Size(max = 200) String name) {
}
