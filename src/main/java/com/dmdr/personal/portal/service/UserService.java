package com.dmdr.personal.portal.service;

import com.dmdr.personal.portal.dto.RegistrationDto;
import com.dmdr.personal.portal.model.User;

import java.util.Optional;

public interface UserService {
    User register(RegistrationDto dto);
    Optional<User> findByEmail(String email);
}
