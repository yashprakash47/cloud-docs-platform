package com.clouddocs.processing.domain;

import java.time.Instant;
import java.util.UUID;

public record ProcessingAttempt(
        UUID id,
        UUID tenantId,
        UUID processingJobId,
        int attemptNumber,
        Instant startedAt,
        Instant endedAt,
        ProcessingAttemptStatus status,
        String errorMessage) {
}
