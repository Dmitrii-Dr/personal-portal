package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.service.JwtService;
import com.dmdr.personal.portal.users.dto.AuthResponse;
import com.dmdr.personal.portal.users.dto.CreateUserRequest;
import com.dmdr.personal.portal.users.dto.LoginRequest;
import com.dmdr.personal.portal.users.model.Role;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!userService.validatePassword(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String token = jwtService.generateToken(user.getEmail(), roles);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setEmail(user.getEmail());
        response.setRoles(roles);

        log.info("User logged in successfully: {}", user.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/registry")
    //TODO probably registry should not return token, but only status code 201
    public ResponseEntity<AuthResponse> registry(@Valid @RequestBody CreateUserRequest request) {
        // Validate that user doesn't exist
        if (userService.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }

        User user = userService.createUser(request);

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String token = jwtService.generateToken(user.getEmail(), roles);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setEmail(user.getEmail());
        response.setRoles(roles);

        log.info("User registered successfully: {}", user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

