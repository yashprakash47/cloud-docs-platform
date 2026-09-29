package com.clouddocs.tenancy.api;

import com.clouddocs.identity.application.AuthenticationService;
import com.clouddocs.identity.domain.User;
import com.clouddocs.tenancy.application.MembershipService;
import com.clouddocs.tenancy.domain.Membership;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/memberships")
public class MembershipController {

    private final AuthenticationService authenticationService;
    private final MembershipService membershipService;

    public MembershipController(AuthenticationService authenticationService, MembershipService membershipService) {
        this.authenticationService = authenticationService;
        this.membershipService = membershipService;
    }

    @GetMapping
    public List<Membership> list(@PathVariable UUID tenantId, HttpServletRequest request) {
        return membershipService.findByTenant(currentUser(request), tenantId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Membership add(@PathVariable UUID tenantId, @Valid @RequestBody CreateMembershipRequest body,
                           HttpServletRequest request) {
        return membershipService.add(currentUser(request), tenantId, body.email(), body.role());
    }

    @PatchMapping("/{membershipId}/role")
    public Membership changeRole(@PathVariable UUID tenantId, @PathVariable UUID membershipId,
                                 @Valid @RequestBody ChangeRoleRequest body, HttpServletRequest request) {
        return membershipService.changeRole(currentUser(request), tenantId, membershipId, body.role());
    }

    @DeleteMapping("/{membershipId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID tenantId, @PathVariable UUID membershipId,
                       HttpServletRequest request) {
        membershipService.remove(currentUser(request), tenantId, membershipId);
    }

    private User currentUser(HttpServletRequest request) {
        return authenticationService.requireCurrentUser(request);
    }
}
