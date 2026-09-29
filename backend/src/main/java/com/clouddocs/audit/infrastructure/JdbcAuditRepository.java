package com.clouddocs.audit.infrastructure;

import com.clouddocs.audit.domain.AuditAction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class JdbcAuditRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(UUID tenantId, UUID actorUserId, AuditAction action, String targetType, UUID targetId) {
        jdbcTemplate.update(
                "INSERT INTO audit_events (id, tenant_id, actor_user_id, action, target_type, target_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), tenantId, actorUserId, action.name(), targetType, targetId);
    }
}
