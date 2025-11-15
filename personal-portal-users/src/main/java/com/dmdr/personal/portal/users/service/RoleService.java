package com.dmdr.personal.portal.users.service;

import com.dmdr.personal.portal.users.model.Role;
import com.dmdr.personal.portal.users.dto.CreateRoleRequest;

public interface RoleService {

    Role createRole(CreateRoleRequest request);
}

