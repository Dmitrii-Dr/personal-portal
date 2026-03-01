package com.dmdr.personal.portal.users.dto;

import com.dmdr.personal.portal.users.model.User;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserResponseForAdmin(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        boolean isVerified,
        Set<String> roles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static UserResponseForAdmin from(User user) {
        Set<String> roleNames = user.getRoles() == null
                ? Set.of()
                : user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toUnmodifiableSet());

        return new UserResponseForAdmin(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.isActive(),
                roleNames,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
