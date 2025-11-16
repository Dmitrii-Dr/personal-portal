package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.content.dto.UserResponse;
import com.dmdr.personal.portal.service.CurrentUserService;
import com.dmdr.personal.portal.users.dto.UpdateUserProfileRequest;
import com.dmdr.personal.portal.users.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;
import com.dmdr.personal.portal.users.service.UserService;

@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    
    private final CurrentUserService currentUserService;
    private final UserService userService;

    public UserController(CurrentUserService currentUserService, UserService userService) {
        this.currentUserService = currentUserService;
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile() {
        User user = currentUserService.getCurrentUser();
        UserResponse dto = new UserResponse();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        User current = currentUserService.getCurrentUser();
        User updated = userService.updateProfile(current.getId(), request);

        UserResponse dto = new UserResponse();
        dto.setId(updated.getId());
        dto.setEmail(updated.getEmail());
        dto.setFirstName(updated.getFirstName());
        dto.setLastName(updated.getLastName());
        dto.setCreatedAt(updated.getCreatedAt());
        dto.setUpdatedAt(updated.getUpdatedAt());
        return ResponseEntity.ok(dto);
    }
}
