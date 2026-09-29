package com.clouddocs.audit.application;

import com.clouddocs.audit.domain.AuditAction;

import java.util.UUID;

public interface AuditService {

    void record(UUID tenantId, UUID actorUserId, AuditAction action, String targetType, UUID targetId);
}
