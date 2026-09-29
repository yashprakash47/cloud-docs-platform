package com.clouddocs.identity.domain;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        String email,
        String displayName,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt) {
}
