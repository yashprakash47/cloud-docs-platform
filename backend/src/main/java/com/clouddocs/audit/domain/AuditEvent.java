package com.clouddocs.audit.domain;

import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
        UUID id,
        UUID tenantId,
        UUID actorUserId,
        AuditAction action,
        String targetType,
        UUID targetId,
        Instant occurredAt) {
}
