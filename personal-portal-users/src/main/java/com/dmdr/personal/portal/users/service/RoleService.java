package com.dmdr.personal.portal.users.service;

import com.dmdr.personal.portal.users.model.Role;
import com.dmdr.personal.portal.users.dto.CreateRoleRequest;

import java.util.Optional;

public interface RoleService {

    Role createRole(CreateRoleRequest request);

    Optional<Role> findByName(String name);
}

