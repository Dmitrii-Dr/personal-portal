package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.core.security.SystemRole;
import com.dmdr.personal.portal.content.dto.UserResponse;
import com.dmdr.personal.portal.users.model.Role;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers() {
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
                    dto.setCreatedAt(user.getCreatedAt());
                    dto.setUpdatedAt(user.getUpdatedAt());
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
