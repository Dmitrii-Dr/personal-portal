package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.users.dto.CreateUserRequest;
import com.dmdr.personal.portal.users.dto.UserResponse;
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

@RestController
@RequestMapping("/api/v1/users")
@Slf4j
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        UserResponse response = UserResponse.from(user);
        log.info("User created successfully: {}", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

