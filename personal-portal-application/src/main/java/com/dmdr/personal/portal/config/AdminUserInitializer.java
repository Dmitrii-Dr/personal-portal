package com.dmdr.personal.portal.config;

import com.dmdr.personal.portal.users.model.Role;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.dto.CreateRoleRequest;
import com.dmdr.personal.portal.users.repository.UserRepository;
import com.dmdr.personal.portal.users.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.user.email}")
    private String adminEmail;

    @Value("${admin.user.password}")
    private String adminPassword;

    public AdminUserInitializer(UserRepository userRepository, RoleService roleService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin user already exists with email: {}", adminEmail);
            return;
        }
        Role adminRole = roleService.findByName(ADMIN_ROLE)
                .orElseGet(() -> roleService.createRole(new CreateRoleRequest(ADMIN_ROLE)));

        User adminUser = new User();
        adminUser.setEmail(adminEmail);
        adminUser.setPassword(passwordEncoder.encode(adminPassword));
        adminUser.addRole(adminRole);

        userRepository.save(adminUser);
        log.info("Admin user created successfully with email: {}", adminEmail);
    }
}

