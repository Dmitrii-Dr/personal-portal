package com.dmdr.personal.portal.users.service.impl;

import com.dmdr.personal.portal.core.email.EmailService;
import com.dmdr.personal.portal.core.security.SystemRole;
import com.dmdr.personal.portal.users.dto.UpdateUserProfileRequest;
import com.dmdr.personal.portal.users.model.Role;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.dto.CreateRoleRequest;
import com.dmdr.personal.portal.users.dto.CreateUserRequest;
import com.dmdr.personal.portal.users.repository.UserRepository;
import com.dmdr.personal.portal.users.service.RoleService;
import com.dmdr.personal.portal.users.service.UserService;
import com.dmdr.personal.portal.users.model.SignedAgreement;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
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
    private final EmailService emailService;
    private final com.dmdr.personal.portal.users.service.UserSettingsService userSettingsService;
    private final com.dmdr.personal.portal.users.service.AgreementVerifier agreementVerifier;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            RoleService roleService, EmailService emailService,
            com.dmdr.personal.portal.users.service.UserSettingsService userSettingsService,
            com.dmdr.personal.portal.users.service.AgreementVerifier agreementVerifier) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
        this.emailService = emailService;
        this.userSettingsService = userSettingsService;
        this.agreementVerifier = agreementVerifier;
    }

    @Override
    public User createUser(CreateUserRequest request) {
        // Check if user with email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }

        // Verify agreements and get snapshots
        List<SignedAgreement> signedAgreements = agreementVerifier
                .verifyAndGetSnapshots(request.getSignedAgreements());

        User user = new User();
        user.setEmail(request.getEmail());
        // Hash the password before saving
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.addSignedAgreements(signedAgreements);

        // Assign ROLE_USER by default (create if it doesn't exist)
        Role userRole = roleService.findByName(DEFAULT_ROLE)
                .orElseGet(() -> roleService.createRole(new CreateRoleRequest(DEFAULT_ROLE)));
        user.addRole(userRole);

        User savedUser = userRepository.save(user);

        // Send welcome email asynchronously (non-blocking) only if email notifications
        // are enabled
        if (userSettingsService.isEmailNotificationEnabled(savedUser.getId())) {
            try {
                emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getFirstName(), savedUser.getLastName());
            } catch (Exception e) {
                // Log error but don't fail user creation if email fails
                System.err.println("Failed to send welcome email to " + savedUser.getEmail() + ": " + e.getMessage());
            }
        }

        return savedUser;
    }

    @Override
    public User createUserByAdmin(String email, String firstName, String lastName, String phoneNumber) {
        // Check if user with email already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("User with email " + email + " already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhoneNumber(phoneNumber);

        String randomPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(randomPassword));

        Role userRole = roleService.findByName(DEFAULT_ROLE)
                .orElseGet(() -> roleService.createRole(new CreateRoleRequest(DEFAULT_ROLE)));
        user.addRole(userRole);

        User savedUser = userRepository.save(user);

        return savedUser;
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
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        return userRepository.save(user);
    }

    @Override
    public void updatePassword(UUID userId, String newPassword) {
        if (userId == null) {
            throw new IllegalArgumentException("User id cannot be null");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password cannot be null or blank");
        }

        User user = userRepository.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User with id " + userId + " not found"));

        // Encode the new password
        user.setPassword(passwordEncoder.encode(newPassword));
        // Set the last password reset date to current time
        user.setLastPasswordResetDate(OffsetDateTime.now());

        userRepository.save(user);
    }

    /**
     * Generates a secure random password with a mix of uppercase, lowercase,
     * digits, and special characters.
     * 
     * @return A random password of 16 characters
     */
    private String generateRandomPassword() {
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*";
        String allChars = uppercase + lowercase + digits + special;

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(16);

        // Ensure at least one character from each category
        password.append(uppercase.charAt(random.nextInt(uppercase.length())));
        password.append(lowercase.charAt(random.nextInt(lowercase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // Fill the rest randomly
        for (int i = 4; i < 16; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the password to avoid predictable pattern
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }

        return new String(passwordArray);
    }
}
