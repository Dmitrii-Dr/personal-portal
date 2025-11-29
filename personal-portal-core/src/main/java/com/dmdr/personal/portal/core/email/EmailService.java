package com.dmdr.personal.portal.core.email;

/**
 * Service interface for sending emails.
 */
public interface EmailService {

    /**
     * Sends a welcome email to a newly created user.
     *
     * @param toEmail the recipient's email address
     * @param firstName the recipient's first name
     * @param lastName the recipient's last name
     */
    void sendWelcomeEmail(String toEmail, String firstName, String lastName);
}

