package com.clouddocs.tenancy.application;

import com.clouddocs.operations.api.ForbiddenOperationException;
import com.clouddocs.tenancy.domain.Role;

public final class AuthorizationRules {

    private AuthorizationRules() {
    }

    public static void requireWriteAccess(Role role) {
        if (role == Role.VIEWER) {
            throw new ForbiddenOperationException("VIEWER cannot perform write operations");
        }
    }

    public static void requireMembershipManagement(Role role) {
        if (role != Role.TENANT_ADMIN) {
            throw new ForbiddenOperationException("Only TENANT_ADMIN can manage memberships");
        }
    }
}
