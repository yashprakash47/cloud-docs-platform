package com.clouddocs.identity.infrastructure;

import com.clouddocs.identity.domain.User;
import com.clouddocs.identity.domain.UserStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcUserRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<User> findByEmail(String email) {
        List<User> users = jdbcTemplate.query(
                "SELECT id, email, display_name, status, created_at, updated_at FROM users WHERE email = ?",
                this::mapUser,
                email);
        return users.stream().findFirst();
    }

    public User insert(String email, String displayName) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, display_name, status) VALUES (?, ?, ?, ?)",
                id, email, displayName, UserStatus.ACTIVE.name());
        return findByEmail(email).orElseThrow();
    }

    private User mapUser(ResultSet resultSet, int rowNumber) throws SQLException {
        return new User(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("email"),
                resultSet.getString("display_name"),
                UserStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }
}
