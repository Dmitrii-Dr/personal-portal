package com.dmdr.personal.portal.users.service;

/**
 * Service for handling password reset operations.
 */
public interface PasswordResetService {

    /**
     * Requests a password reset for the given email address.
     * If the user exists, a reset token is generated and sent via email.
     * For security reasons, this method always returns successfully even if the user is not found
     * to prevent email enumeration attacks.
     *
     * @param email the email address of the user requesting password reset
     */
    void requestPasswordReset(String email);

    /**
     * Resets the password using a valid reset token.
     *
     * @param token the password reset token
     * @param email the email address to validate against the token
     * @param newPassword the new password to set
     * @throws IllegalArgumentException if the token is invalid, expired, or email doesn't match
     */
    void resetPassword(String token, String email, String newPassword);
}

