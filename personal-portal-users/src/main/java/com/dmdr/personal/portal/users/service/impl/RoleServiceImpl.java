package com.dmdr.personal.portal.users.service.impl;

import com.dmdr.personal.portal.core.user.domain.Role;
import com.dmdr.personal.portal.users.dto.CreateRoleRequest;
import com.dmdr.personal.portal.users.repository.RoleRepository;
import com.dmdr.personal.portal.users.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Role createRole(CreateRoleRequest request) {
        Role role = new Role(request.getName());
        return roleRepository.save(role);
    }
}

