package com.dmdr.personal.portal.users.service.impl;

import com.dmdr.personal.portal.core.email.EmailService;
import com.dmdr.personal.portal.users.model.PasswordResetToken;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.repository.PasswordResetTokenRepository;
import com.dmdr.personal.portal.users.service.PasswordResetService;
import com.dmdr.personal.portal.users.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int TOKEN_EXPIRY_MINUTES = 15;

    private final UserService userService;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final String frontendUrl;

    public PasswordResetServiceImpl(
            UserService userService,
            PasswordResetTokenRepository tokenRepository,
            EmailService emailService,
            @Value("${app.frontend.url}") String frontendUrl) {
        this.userService = userService;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void requestPasswordReset(String email) {
        Optional<User> userOpt = userService.findByEmail(email);

        // Security: Always return success even if user not found to prevent email enumeration
        if (userOpt.isEmpty()) {
            log.debug("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = userOpt.get();

        // Delete existing tokens for this user if any exist
        tokenRepository.deleteByUser(user);

        // Create new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(TOKEN_EXPIRY_MINUTES);
        tokenRepository.save(resetToken);

        // Generate reset link
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        // Send email
        try {
            emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    resetLink
            );
            log.info("Password reset email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", email, e.getMessage());
            // Don't throw - email failure shouldn't break the flow
        }
    }

    @Override
    public void resetPassword(String token, String email, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired token.");
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Invalid or expired token.");
        }

        User user = resetToken.getUser();

        // Validate that the provided email matches the token's user email
        if (!user.getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("Email does not match.");
        }

        // Update password (this also sets lastPasswordResetDate)
        userService.updatePassword(user.getId(), newPassword);

        // Clean up token (Important!)
        tokenRepository.delete(resetToken);

        log.info("Password successfully reset for user: {}", user.getEmail());
    }
}

