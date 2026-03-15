package com.dmdr.personal.portal.service;

import com.dmdr.personal.portal.core.security.SystemRole;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {

    private final UserService userService;

    public CurrentUserService(UserService userService) {
        this.userService = userService;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("User must be authenticated");
        }

        String userIdRaw = authentication.getName();
        UUID userId = UUID.fromString(userIdRaw);
        return userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    public boolean isCurrentUserAdmin() {
        try {
            User user = getCurrentUser();
            if (user.getRoles() == null) {
                return false;
            }
            return user.getRoles().stream()
                    .anyMatch(role -> SystemRole.ADMIN.getAuthority().equals(role.getName()));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
