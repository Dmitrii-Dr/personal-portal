package com.dmdr.personal.portal.users.service;

import com.dmdr.personal.portal.users.dto.CreateUserRequest;
import com.dmdr.personal.portal.users.dto.UpdateUserProfileRequest;
import com.dmdr.personal.portal.users.model.User;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserService {

    User createUser(CreateUserRequest request);

    /**
     * Creates a user by admin. Used for admin-created users.
     *
     * @param email user email (required)
     * @param firstName user first name (optional)
     * @param lastName user last name (optional)
     * @return the created user
     * @throws IllegalArgumentException if user with email already exists
     */
    User createUserByAdmin(String email, String firstName, String lastName);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    List<User> findByIds(Set<UUID> ids);

    List<User> findByRoleName(String roleName, Comparator<User> comparator);

    boolean validatePassword(String rawPassword, String encodedPassword);

    User updateProfile(UUID userId, UpdateUserProfileRequest request);
}

