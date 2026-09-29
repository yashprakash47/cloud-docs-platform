package com.clouddocs.document.domain;

import java.time.Instant;
import java.util.UUID;

public record DocumentVersion(
        UUID id,
        UUID tenantId,
        UUID documentId,
        int versionNumber,
        String storageReference,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String checksum,
        UUID uploadedBy,
        Instant createdAt) {
}
