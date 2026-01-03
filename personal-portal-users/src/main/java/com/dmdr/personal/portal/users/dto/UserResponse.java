package com.dmdr.personal.portal.users.dto;

import com.dmdr.personal.portal.users.model.User;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        Set<String> roles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static UserResponse from(User user) {
        Set<String> roleNames = user.getRoles() == null
                ? Set.of()
                : user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toUnmodifiableSet());

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                roleNames,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
