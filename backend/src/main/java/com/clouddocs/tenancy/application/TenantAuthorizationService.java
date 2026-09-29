package com.clouddocs.tenancy.application;

import com.clouddocs.identity.domain.User;
import com.clouddocs.operations.api.ForbiddenOperationException;
import com.clouddocs.operations.api.ResourceNotFoundException;
import com.clouddocs.tenancy.domain.Membership;
import com.clouddocs.tenancy.domain.Role;
import com.clouddocs.tenancy.infrastructure.JdbcMembershipRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TenantAuthorizationService {

    private final JdbcMembershipRepository membershipRepository;

    public TenantAuthorizationService(JdbcMembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    public Membership requireMembership(User user, UUID tenantId) {
        return membershipRepository.findByUserAndTenant(user.id(), tenantId)
                .orElseThrow(() -> new ForbiddenOperationException("User cannot access this tenant"));
    }

    public Membership requireMembershipInTenant(UUID membershipId, UUID tenantId) {
        return membershipRepository.findByIdAndTenant(membershipId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found in this tenant"));
    }

    public Role requireWriteAccess(User user, UUID tenantId) {
        Membership membership = requireMembership(user, tenantId);
        AuthorizationRules.requireWriteAccess(membership.role());
        return membership.role();
    }

    public void requireMembershipManagement(User user, UUID tenantId) {
        Membership membership = requireMembership(user, tenantId);
        AuthorizationRules.requireMembershipManagement(membership.role());
    }
}
