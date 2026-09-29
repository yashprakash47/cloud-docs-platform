package com.clouddocs.document.domain;

import java.time.Instant;
import java.util.UUID;

public record Tag(
        UUID id,
        UUID tenantId,
        String name,
        Instant createdAt,
        Instant updatedAt) {
}
