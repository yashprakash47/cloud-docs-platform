package com.clouddocs.tenancy.domain;

import java.time.Instant;
import java.util.UUID;

public record Membership(
        UUID id,
        UUID userId,
        UUID tenantId,
        Role role,
        Instant createdAt,
        Instant updatedAt) {
}
