package com.clouddocs.document.domain;

import java.time.Instant;
import java.util.UUID;

public record Document(
        UUID id,
        UUID tenantId,
        UUID ownerUserId,
        UUID folderId,
        String name,
        String description,
        DocumentStatus status,
        UUID currentVersionId,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt) {
}
