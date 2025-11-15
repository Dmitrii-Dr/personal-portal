package com.dmdr.personal.portal.users.service.impl;

import com.dmdr.personal.portal.users.model.Role;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.dto.CreateRoleRequest;
import com.dmdr.personal.portal.users.dto.CreateUserRequest;
import com.dmdr.personal.portal.users.repository.UserRepository;
import com.dmdr.personal.portal.users.service.RoleService;
import com.dmdr.personal.portal.users.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, RoleService roleService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
    }

    @Override
    public User createUser(CreateUserRequest request) {
        // Check if user with email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        // Hash the password before saving
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Assign ROLE_USER by default (create if it doesn't exist)
        Role userRole = roleService.findByName(DEFAULT_ROLE)
                .orElseGet(() -> roleService.createRole(new CreateRoleRequest(DEFAULT_ROLE)));
        user.addRole(userRole);

        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}

