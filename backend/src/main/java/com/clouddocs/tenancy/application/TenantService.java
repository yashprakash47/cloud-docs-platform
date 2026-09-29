package com.clouddocs.tenancy.application;

import com.clouddocs.identity.domain.User;
import com.clouddocs.tenancy.domain.Role;
import com.clouddocs.tenancy.domain.Tenant;
import com.clouddocs.tenancy.infrastructure.JdbcMembershipRepository;
import com.clouddocs.tenancy.infrastructure.JdbcTenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TenantService {

    private final JdbcTenantRepository tenantRepository;
    private final JdbcMembershipRepository membershipRepository;

    public TenantService(JdbcTenantRepository tenantRepository, JdbcMembershipRepository membershipRepository) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
    }

    public List<Tenant> findAccessibleTenants(User user) {
        return tenantRepository.findAccessibleByUser(user.id());
    }

    @Transactional
    public Tenant createTenant(User user, String name) {
        Tenant tenant = tenantRepository.insert(name.trim());
        membershipRepository.insert(user.id(), tenant.id(), Role.TENANT_ADMIN);
        return tenant;
    }
}
