package com.dmdr.personal.portal.users.service.impl;

import com.dmdr.personal.portal.core.security.SystemRole;
import com.dmdr.personal.portal.users.dto.UpdateUserProfileRequest;
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
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE = SystemRole.USER.getAuthority();

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
    public Optional<User> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return userRepository.findUserById(id);
    }

    @Override
    public List<User> findByIds(Set<UUID> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        return userRepository.findAllById(ids);
    }

    @Override
    public List<User> findByRoleName(String roleName, Comparator<User> comparator) {
        if (roleName == null || roleName.isBlank()) {
            return List.of();
        }
        List<User> users = userRepository.findByRoles_Name(roleName);
        if (comparator == null) {
            return users;
        }
        return users.stream().sorted(comparator).collect(Collectors.toList());
    }

    @Override
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public User updateProfile(UUID userId, UpdateUserProfileRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User id cannot be null");
        }
        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + userId + " not found"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        return userRepository.save(user);
    }
}

