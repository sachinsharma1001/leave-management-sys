package com.assignment.auth.service;

import com.assignment.auth.model.AppUser;
import com.assignment.auth.model.Role;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserDirectory {

    private final PasswordEncoder encoder;
    private final Map<String, AppUser> users = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        add(new AppUser(1L, "employee1", encoder.encode("password"), Role.EMPLOYEE, 100L));
        add(new AppUser(2L, "employee2", encoder.encode("password"), Role.EMPLOYEE, 100L));
        add(new AppUser(100L, "manager1", encoder.encode("password"), Role.MANAGER, null));
    }

    private void add(AppUser user) {
        users.put(user.getUsername(), user);
    }

    public Optional<AppUser> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }
}
