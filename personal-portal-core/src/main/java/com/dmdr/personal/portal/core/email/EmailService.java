package com.dmdr.personal.portal.core.email;

import java.time.Instant;

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

    /**
     * Sends a booking confirmation email to the client.
     *
     * @param toEmail the recipient's email address
     * @param firstName the recipient's first name
     * @param lastName the recipient's last name
     * @param sessionTypeName the name of the session type
     * @param startTime the start time of the booking
     */
    void sendBookingConfirmationEmail(String toEmail, String firstName, String lastName, 
                                     String sessionTypeName, Instant startTime);

    /**
     * Sends a booking rejection email to the client.
     *
     * @param toEmail the recipient's email address
     * @param firstName the recipient's first name
     * @param lastName the recipient's last name
     * @param sessionTypeName the name of the session type
     * @param startTime the start time of the booking
     */
    void sendBookingRejectionEmail(String toEmail, String firstName, String lastName,
                                  String sessionTypeName, Instant startTime);

    /**
     * Sends a new booking request notification email to an admin user.
     *
     * @param toEmail the recipient's email address
     * @param clientName the client's full name
     * @param clientEmail the client's email address
     * @param sessionTypeName the name of the session type
     * @param startTime the start time of the booking
     * @param clientMessage the optional message from the client
     */
    void sendBookingRequestAdminEmail(String toEmail, String clientName, String clientEmail,
                                     String sessionTypeName, Instant startTime, String clientMessage);

    /**
     * Sends a password reset email to the user.
     *
     * @param toEmail the recipient's email address
     * @param firstName the recipient's first name
     * @param lastName the recipient's last name
     * @param resetLink the password reset link with token
     */
    void sendPasswordResetEmail(String toEmail, String firstName, String lastName, String resetLink);
}

