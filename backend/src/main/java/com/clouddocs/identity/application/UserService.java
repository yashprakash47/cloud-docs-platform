package com.clouddocs.identity.application;

import com.clouddocs.identity.domain.User;

import java.util.Optional;

public interface UserService {

    Optional<User> findByEmail(String email);

    User provisionLocalUser(String email);
}
