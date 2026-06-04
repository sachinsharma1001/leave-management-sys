package com.assignment.auth.controller;

import com.assignment.auth.dto.LoginRequest;
import com.assignment.auth.dto.LoginResponse;
import com.assignment.auth.model.AppUser;
import com.assignment.auth.service.JwtService;
import com.assignment.auth.service.UserDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserDirectory users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        AppUser user = users.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!encoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        return LoginResponse.builder()
                .token(jwtService.generateToken(user))
                .userId(user.getId())
                .role(user.getRole().name())
                .expiresInSeconds(jwtService.expiresInSeconds())
                .build();
    }
}
