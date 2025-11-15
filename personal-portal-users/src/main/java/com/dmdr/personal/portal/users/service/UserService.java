package com.dmdr.personal.portal.users.service;

import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.dto.CreateUserRequest;

public interface UserService {

    User createUser(CreateUserRequest request);
}

