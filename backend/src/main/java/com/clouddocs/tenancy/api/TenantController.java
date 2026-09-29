package com.clouddocs.tenancy.api;

import com.clouddocs.identity.application.AuthenticationService;
import com.clouddocs.identity.domain.User;
import com.clouddocs.tenancy.application.TenantService;
import com.clouddocs.tenancy.domain.Tenant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final AuthenticationService authenticationService;
    private final TenantService tenantService;

    public TenantController(AuthenticationService authenticationService, TenantService tenantService) {
        this.authenticationService = authenticationService;
        this.tenantService = tenantService;
    }

    @GetMapping
    public List<Tenant> accessibleTenants(HttpServletRequest request) {
        return tenantService.findAccessibleTenants(authenticationService.requireCurrentUser(request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Tenant createTenant(@Valid @RequestBody CreateTenantRequest body, HttpServletRequest request) {
        return tenantService.createTenant(authenticationService.requireCurrentUser(request), body.name());
    }
}
