package com.dmdr.personal.portal.users.service;

import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.dto.CreateUserRequest;

import java.util.Optional;

public interface UserService {

    User createUser(CreateUserRequest request);

    Optional<User> findByEmail(String email);

    boolean validatePassword(String rawPassword, String encodedPassword);
}

