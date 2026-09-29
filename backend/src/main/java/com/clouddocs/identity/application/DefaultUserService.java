package com.clouddocs.identity.application;

import com.clouddocs.identity.domain.User;
import com.clouddocs.identity.infrastructure.JdbcUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class DefaultUserService implements UserService {

    private final JdbcUserRepository userRepository;

    public DefaultUserService(JdbcUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    @Override
    @Transactional
    public User provisionLocalUser(String email) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> userRepository.insert(normalizedEmail, displayNameFor(normalizedEmail)));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String displayNameFor(String email) {
        return email.substring(0, email.indexOf('@'));
    }
}
