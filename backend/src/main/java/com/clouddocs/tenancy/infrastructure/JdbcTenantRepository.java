package com.clouddocs.tenancy.infrastructure;

import com.clouddocs.tenancy.domain.Tenant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcTenantRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTenantRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Tenant insert(String name) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO tenants (id, name) VALUES (?, ?)", id, name);
        return findById(id).orElseThrow();
    }

    public Optional<Tenant> findById(UUID id) {
        List<Tenant> tenants = jdbcTemplate.query(
                "SELECT id, name, created_at, updated_at FROM tenants WHERE id = ?",
                this::mapTenant,
                id);
        return tenants.stream().findFirst();
    }

    public List<Tenant> findAccessibleByUser(UUID userId) {
        return jdbcTemplate.query(
                "SELECT t.id, t.name, t.created_at, t.updated_at "
                        + "FROM tenants t JOIN memberships m ON m.tenant_id = t.id "
                        + "WHERE m.user_id = ? ORDER BY t.name",
                this::mapTenant,
                userId);
    }

    private Tenant mapTenant(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Tenant(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("name"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }
}
