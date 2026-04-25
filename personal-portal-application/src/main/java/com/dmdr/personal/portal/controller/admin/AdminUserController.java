package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.core.security.SystemRole;
import com.dmdr.personal.portal.content.dto.UserResponse;
import com.dmdr.personal.portal.users.dto.UpdateUserAdminRequest;
import com.dmdr.personal.portal.users.dto.UpdateUserSettingsRequest;
import com.dmdr.personal.portal.users.dto.UserSettingsResponse;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.service.UserService;
import com.dmdr.personal.portal.users.service.UserSettingsService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final UserService userService;
    private final UserSettingsService userSettingsService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers(HttpServletRequest httpRequest) {
        String ctx = AdminApiLogSupport.http(httpRequest);
        log.info("BEGIN getUsers {}", ctx);
        try {
            Comparator<User> comparator = Comparator.comparing(
                    u -> u.getLastName() == null ? "" : u.getLastName(),
                    String.CASE_INSENSITIVE_ORDER
            );
            List<User> users = userService.findByRoleName(SystemRole.USER.getAuthority(), comparator);
            List<UserResponse> response = users.stream()
                    .map(user -> {
                        UserResponse dto = new UserResponse();
                        dto.setId(user.getId());
                        dto.setEmail(user.getEmail());
                        dto.setFirstName(user.getFirstName());
                        dto.setLastName(user.getLastName());
                        dto.setPhoneNumber(user.getPhoneNumber());
                        dto.setVerified(user.isActive());
                        dto.setLocked(user.isLocked());
                        dto.setCreatedAt(user.getCreatedAt());
                        dto.setUpdatedAt(user.getUpdatedAt());
                        return dto;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } finally {
            log.info("END getUsers {}", ctx);
        }
    }

    @GetMapping("/{userId}/settings")
    public ResponseEntity<UserSettingsResponse> getUserSettings(@PathVariable UUID userId) {
        String ctx = "userId=" + userId;
        log.info("BEGIN getUserSettings {}", ctx);
        try {
            validateClientUser(userId);
            UserSettingsResponse response = userSettingsService.getSettings(userId);
            return ResponseEntity.ok(response);
        } finally {
            log.info("END getUserSettings {}", ctx);
        }
    }

    @PutMapping("/{userId}/settings")
    public ResponseEntity<UserSettingsResponse> updateUserSettings(@PathVariable UUID userId,
            @Valid @RequestBody UpdateUserSettingsRequest request,
            HttpServletRequest httpRequest) {
        String ctx = AdminApiLogSupport.http(httpRequest) + " userId=" + userId;
        log.info("BEGIN updateUserSettings {}", ctx);
        try {
            validateClientUser(userId);
            UserSettingsResponse response = userSettingsService.updateSettings(userId, request);
            return ResponseEntity.ok(response);
        } finally {
            log.info("END updateUserSettings {}", ctx);
        }
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable UUID userId,
            @Valid @RequestBody UpdateUserAdminRequest request,
            HttpServletRequest httpRequest) {
        String ctx = AdminApiLogSupport.http(httpRequest) + " userId=" + userId;
        log.info("BEGIN updateUser {}", ctx);
        try {
            User user = userService.updateUserByAdmin(userId, request);
            return ResponseEntity.ok(toResponse(user));
        } finally {
            log.info("END updateUser {}", ctx);
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId, HttpServletRequest httpRequest) {
        String ctx = AdminApiLogSupport.http(httpRequest) + " userId=" + userId;
        log.info("BEGIN deleteUser {}", ctx);
        try {
            userService.deleteUserByAdmin(userId);
            return ResponseEntity.noContent().build();
        } finally {
            log.info("END deleteUser {}", ctx);
        }
    }

    private UserResponse toResponse(User user) {
        UserResponse dto = new UserResponse();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setVerified(user.isActive());
        dto.setLocked(user.isLocked());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }

    private void validateClientUser(UUID userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + userId + " not found"));
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> SystemRole.ADMIN.getAuthority().equals(role.getName()));
        if (isAdmin) {
            throw new IllegalArgumentException("Admin users cannot be managed as clients");
        }
        boolean isClient = user.getRoles().stream()
                .anyMatch(role -> SystemRole.USER.getAuthority().equals(role.getName()));
        if (!isClient) {
            throw new IllegalArgumentException("User is not a client");
        }
    }
}
