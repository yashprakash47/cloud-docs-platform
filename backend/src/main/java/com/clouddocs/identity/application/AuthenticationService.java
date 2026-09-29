package com.clouddocs.identity.application;

import com.clouddocs.identity.domain.User;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthenticationService {

    User requireCurrentUser(HttpServletRequest request);
}
