package com.clouddocs.processing.domain;

import java.time.Instant;
import java.util.UUID;

public record ProcessingJob(
        UUID id,
        UUID tenantId,
        UUID documentId,
        UUID documentVersionId,
        ProcessingJobStatus status,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        String failureReason,
        int attemptCount,
        String messageId) {
}
