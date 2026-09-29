package com.clouddocs.tenancy.application;

import com.clouddocs.identity.application.UserService;
import com.clouddocs.identity.domain.User;
import com.clouddocs.operations.api.ResourceNotFoundException;
import com.clouddocs.tenancy.domain.Membership;
import com.clouddocs.tenancy.domain.Role;
import com.clouddocs.tenancy.infrastructure.JdbcMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MembershipService {

    private final JdbcMembershipRepository membershipRepository;
    private final UserService userService;
    private final TenantAuthorizationService authorizationService;

    public MembershipService(JdbcMembershipRepository membershipRepository, UserService userService,
                             TenantAuthorizationService authorizationService) {
        this.membershipRepository = membershipRepository;
        this.userService = userService;
        this.authorizationService = authorizationService;
    }

    public List<Membership> findByTenant(User actor, UUID tenantId) {
        authorizationService.requireMembership(actor, tenantId);
        return membershipRepository.findByTenant(tenantId);
    }

    @Transactional
    public Membership add(User actor, UUID tenantId, String email, Role role) {
        authorizationService.requireMembershipManagement(actor, tenantId);
        User member = userService.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User is not provisioned"));
        return membershipRepository.insert(member.id(), tenantId, role);
    }

    @Transactional
    public Membership changeRole(User actor, UUID tenantId, UUID membershipId, Role role) {
        authorizationService.requireMembershipManagement(actor, tenantId);
        authorizationService.requireMembershipInTenant(membershipId, tenantId);
        return membershipRepository.updateRole(membershipId, tenantId, role);
    }

    @Transactional
    public void remove(User actor, UUID tenantId, UUID membershipId) {
        authorizationService.requireMembershipManagement(actor, tenantId);
        authorizationService.requireMembershipInTenant(membershipId, tenantId);
        membershipRepository.delete(membershipId, tenantId);
    }
}
