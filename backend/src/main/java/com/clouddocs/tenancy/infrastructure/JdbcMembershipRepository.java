package com.clouddocs.tenancy.infrastructure;

import com.clouddocs.tenancy.domain.Membership;
import com.clouddocs.tenancy.domain.Role;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcMembershipRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMembershipRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Membership> findByUserAndTenant(UUID userId, UUID tenantId) {
        List<Membership> memberships = jdbcTemplate.query(
                "SELECT id, user_id, tenant_id, role, created_at, updated_at "
                        + "FROM memberships WHERE user_id = ? AND tenant_id = ?",
                this::mapMembership,
                userId,
                tenantId);
        return memberships.stream().findFirst();
    }

    public Optional<Membership> findByIdAndTenant(UUID membershipId, UUID tenantId) {
        List<Membership> memberships = jdbcTemplate.query(
                "SELECT id, user_id, tenant_id, role, created_at, updated_at "
                        + "FROM memberships WHERE id = ? AND tenant_id = ?",
                this::mapMembership,
                membershipId,
                tenantId);
        return memberships.stream().findFirst();
    }

    public List<Membership> findByTenant(UUID tenantId) {
        return jdbcTemplate.query(
                "SELECT id, user_id, tenant_id, role, created_at, updated_at "
                        + "FROM memberships WHERE tenant_id = ? ORDER BY created_at",
                this::mapMembership,
                tenantId);
    }

    public Membership insert(UUID userId, UUID tenantId, Role role) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO memberships (id, user_id, tenant_id, role) VALUES (?, ?, ?, ?)",
                id, userId, tenantId, role.name());
        return findByIdAndTenant(id, tenantId).orElseThrow();
    }

    public Membership updateRole(UUID membershipId, UUID tenantId, Role role) {
        jdbcTemplate.update(
                "UPDATE memberships SET role = ?, updated_at = CURRENT_TIMESTAMP "
                        + "WHERE id = ? AND tenant_id = ?",
                role.name(), membershipId, tenantId);
        return findByIdAndTenant(membershipId, tenantId).orElseThrow();
    }

    public void delete(UUID membershipId, UUID tenantId) {
        jdbcTemplate.update("DELETE FROM memberships WHERE id = ? AND tenant_id = ?", membershipId, tenantId);
    }

    private Membership mapMembership(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Membership(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getObject("tenant_id", UUID.class),
                Role.valueOf(resultSet.getString("role")),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }
}
