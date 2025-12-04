package com.dmdr.personal.portal.config;

import com.dmdr.personal.portal.core.security.SystemRole;
import com.dmdr.personal.portal.users.model.Role;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.dto.CreateRoleRequest;
import com.dmdr.personal.portal.users.dto.CreateUserSettingsRequest;
import com.dmdr.personal.portal.users.repository.UserRepository;
import com.dmdr.personal.portal.users.repository.UserSettingsRepository;
import com.dmdr.personal.portal.users.service.RoleService;
import com.dmdr.personal.portal.users.service.UserSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final UserSettingsService userSettingsService;
    private final UserSettingsRepository userSettingsRepository;

    @Value("${admin.user.email}")
    private String adminEmail;

    @Value("${admin.user.password}")
    private String adminPassword;

    @Value("${admin.user.settings.timezone:UTC}")
    private String adminTimezone;

    @Value("${admin.user.settings.language:en}")
    private String adminLanguage;

    public AdminUserInitializer(
            UserRepository userRepository,
            RoleService roleService,
            PasswordEncoder passwordEncoder,
            UserSettingsService userSettingsService,
            UserSettingsRepository userSettingsRepository) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.userSettingsService = userSettingsService;
        this.userSettingsRepository = userSettingsRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        User existingAdmin = userRepository.findByEmail(adminEmail).orElse(null);
        if (existingAdmin != null) {
            log.info("Admin user already exists with email: {}", adminEmail);
            // Create default settings if they don't exist
            if (userSettingsRepository.findByUserId(existingAdmin.getId()).isEmpty()) {
                createDefaultSettings(existingAdmin.getId());
            } else {
                log.info("Admin user settings already exist");
            }
            return;
        }
        Role adminRole = roleService.findByName(SystemRole.ADMIN.getAuthority())
                .orElseGet(() -> roleService.createRole(new CreateRoleRequest(SystemRole.ADMIN.getAuthority())));

        User adminUser = new User();
        adminUser.setEmail(adminEmail);
        adminUser.setPassword(passwordEncoder.encode(adminPassword));
        adminUser.addRole(adminRole);

        User savedAdmin = userRepository.save(adminUser);
        log.info("Admin user created successfully with email: {}", adminEmail);
        
        // Create default settings for the admin user
        createDefaultSettings(savedAdmin.getId());
    }

    private void createDefaultSettings(java.util.UUID userId) {
        CreateUserSettingsRequest settingsRequest = new CreateUserSettingsRequest();
        settingsRequest.setTimezone(adminTimezone);
        settingsRequest.setLanguage(adminLanguage);
        
        try {
            userSettingsService.createSettings(userId, settingsRequest);
            log.info("Default settings created for admin user with timezone: {}, language: {}", adminTimezone, adminLanguage);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create default settings for admin user: {}", e.getMessage());
        }
    }
}

