package com.clouddocs.identity.api;

import com.clouddocs.identity.application.AuthenticationService;
import com.clouddocs.identity.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class UserController {

    private final AuthenticationService authenticationService;

    public UserController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @GetMapping
    public User currentUser(HttpServletRequest request) {
        return authenticationService.requireCurrentUser(request);
    }
}
