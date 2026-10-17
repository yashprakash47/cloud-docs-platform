package com.clouddocs.tenancy.domain;

import java.time.Instant;
import java.util.UUID;

public record Membership(
        UUID id,
        UUID userId,
        UUID tenantId,
        Role role,
        Instant createdAt,
        Instant updatedAt,
        String userEmail) {

    public Membership(UUID id, UUID userId, UUID tenantId, Role role, Instant createdAt, Instant updatedAt) {
        this(id, userId, tenantId, role, createdAt, updatedAt, null);
    }
}
