package com.clouddocs.document.domain;

import java.time.Instant;
import java.util.UUID;

public record Folder(
        UUID id,
        UUID tenantId,
        UUID parentId,
        String name,
        Instant createdAt,
        Instant updatedAt) {
}
