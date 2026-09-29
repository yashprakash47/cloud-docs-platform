package com.clouddocs.tenancy;

import com.clouddocs.operations.api.ForbiddenOperationException;
import com.clouddocs.tenancy.application.AuthorizationRules;
import com.clouddocs.tenancy.domain.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthorizationRulesTest {

    @Test
    void viewerCannotWrite() {
        assertThrows(ForbiddenOperationException.class,
                () -> AuthorizationRules.requireWriteAccess(Role.VIEWER));
    }

    @Test
    void memberCanWrite() {
        assertDoesNotThrow(() -> AuthorizationRules.requireWriteAccess(Role.MEMBER));
    }

    @Test
    void onlyTenantAdminCanManageMemberships() {
        assertDoesNotThrow(() -> AuthorizationRules.requireMembershipManagement(Role.TENANT_ADMIN));
        assertThrows(ForbiddenOperationException.class,
                () -> AuthorizationRules.requireMembershipManagement(Role.MEMBER));
        assertThrows(ForbiddenOperationException.class,
                () -> AuthorizationRules.requireMembershipManagement(Role.VIEWER));
    }
}
