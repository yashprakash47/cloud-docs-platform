package com.clouddocs.identity.application;

import com.clouddocs.identity.domain.User;
import com.clouddocs.identity.domain.UserStatus;
import com.clouddocs.operations.api.AuthenticationRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@ConfigurationProperties(prefix = "clouddocs.auth.local")
public class LocalAuthenticationService implements AuthenticationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserService userService;
    private String userHeader = "X-User-Email";
    private boolean autoProvision;

    public LocalAuthenticationService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public User requireCurrentUser(HttpServletRequest request) {
        String email = request.getHeader(userHeader);
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new AuthenticationRequiredException("A valid " + userHeader + " header is required");
        }
        User user = autoProvision
                ? userService.provisionLocalUser(email)
                : userService.findByEmail(email)
                        .orElseThrow(() -> new AuthenticationRequiredException("User is not provisioned"));
        if (user.status() != UserStatus.ACTIVE) {
            throw new AuthenticationRequiredException("User is inactive");
        }
        return user;
    }

    public void setUserHeader(String userHeader) {
        this.userHeader = userHeader;
    }

    public void setAutoProvision(boolean autoProvision) {
        this.autoProvision = autoProvision;
    }
}
