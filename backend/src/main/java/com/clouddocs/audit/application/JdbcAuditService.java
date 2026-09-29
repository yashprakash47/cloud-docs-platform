package com.clouddocs.audit.application;

import com.clouddocs.audit.domain.AuditAction;
import com.clouddocs.audit.infrastructure.JdbcAuditRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JdbcAuditService implements AuditService {

    private final JdbcAuditRepository auditRepository;

    public JdbcAuditService(JdbcAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public void record(UUID tenantId, UUID actorUserId, AuditAction action, String targetType, UUID targetId) {
        auditRepository.insert(tenantId, actorUserId, action, targetType, targetId);
    }
}
