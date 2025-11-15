package com.dmdr.personal.portal.users.service;

import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.dto.CreateUserRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserService {

    User createUser(CreateUserRequest request);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    List<User> findByIds(Set<UUID> ids);

    boolean validatePassword(String rawPassword, String encodedPassword);
}

