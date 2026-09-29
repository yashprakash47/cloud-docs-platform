package com.clouddocs.tenancy.api;

import com.clouddocs.tenancy.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMembershipRequest(@NotBlank @Email String email, @NotNull Role role) {
}
