package com.dmdr.personal.portal.core.email;

import java.time.Instant;
import java.time.ZoneId;

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
                                     String sessionTypeName, Instant startTime, ZoneId recipientZoneId);

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
                                  String sessionTypeName, Instant startTime, ZoneId recipientZoneId);

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
                                     String sessionTypeName, Instant startTime, String clientMessage, ZoneId recipientZoneId);

    /**
     * Sends booking-request-received email to the client.
     *
     * @param toEmail the recipient's email address
     * @param firstName the recipient's first name
     * @param lastName the recipient's last name
     * @param sessionTypeName the name of the session type
     * @param startTime the requested start time of the booking
     * @param clientMessage the optional message from the client
     */
    void sendBookingRequestUserEmail(String toEmail, String firstName, String lastName,
            String sessionTypeName, Instant startTime, String clientMessage, ZoneId recipientZoneId);

    /**
     * Sends booking-update-request-received email to the client.
     *
     * @param toEmail the recipient's email address
     * @param firstName the recipient's first name
     * @param lastName the recipient's last name
     * @param sessionTypeName the name of the session type
     * @param oldStartTime the previously requested start time
     * @param newStartTime the newly requested start time
     */
    void sendBookingUpdateRequestUserEmail(String toEmail, String firstName, String lastName,
            String sessionTypeName, Instant oldStartTime, Instant newStartTime, ZoneId recipientZoneId);

    /**
     * Sends booking-update-request notification to an admin user.
     *
     * @param toEmail the recipient's email address
     * @param clientName the client's full name
     * @param clientEmail the client's email address
     * @param sessionTypeName the name of the session type
     * @param oldStartTime the previously requested start time
     * @param newStartTime the newly requested start time
     */
    void sendBookingUpdateRequestAdminEmail(String toEmail, String clientName, String clientEmail,
            String sessionTypeName, Instant oldStartTime, Instant newStartTime, ZoneId recipientZoneId);

    /**
     * Sends booking-updated-by-admin notification to the client.
     *
     * @param toEmail the recipient's email address
     * @param firstName the recipient's first name
     * @param lastName the recipient's last name
     * @param sessionTypeName the name of the session type
     * @param oldStartTime the previous start time before admin update
     * @param newStartTime the new start time set by admin
     */
    void sendBookingUpdatedByAdminUserEmail(String toEmail, String firstName, String lastName,
            String sessionTypeName, Instant oldStartTime, Instant newStartTime, ZoneId recipientZoneId);

    /**
     * Sends a booking cancellation confirmation email to the client.
     *
     * @param toEmail the recipient's email address
     * @param firstName the recipient's first name
     * @param lastName the recipient's last name
     * @param sessionTypeName the name of the session type
     * @param startTime the start time of the cancelled booking
     */
    void sendBookingCancellationUserEmail(String toEmail, String firstName, String lastName,
            String sessionTypeName, Instant startTime, ZoneId recipientZoneId);

    /**
     * Sends a booking cancellation notification email to an admin user.
     *
     * @param toEmail the recipient's email address
     * @param clientName the client's full name
     * @param clientEmail the client's email address
     * @param sessionTypeName the name of the session type
     * @param startTime the start time of the cancelled booking
     */
    void sendBookingCancellationAdminEmail(String toEmail, String clientName, String clientEmail,
            String sessionTypeName, Instant startTime, ZoneId recipientZoneId);

    /**
     * Sends a password reset email to the user.
     *
     * @param toEmail the recipient's email address
     * @param firstName the recipient's first name
     * @param lastName the recipient's last name
     * @param resetLink the password reset link with token
     */
    void sendPasswordResetEmail(String toEmail, String firstName, String lastName, String resetLink);

    /**
     * Sends account verification code email to the user.
     *
     * @param toEmail the recipient's email
     * @param firstName the recipient's first name
     * @param lastName the recipient's last name
     * @param verificationCode one-time numeric verification code
     * @param expiryMinutes code validity period in minutes
     */
    void sendAccountVerificationCodeEmail(
            String toEmail,
            String firstName,
            String lastName,
            String verificationCode,
            int expiryMinutes);

    /**
     * Sends a notification to an admin that the application service has started.
     *
     * @param toEmail the admin recipient address
     * @param startedAt instant of startup; rendered in the template in UTC
     */
    void sendAdminServiceStartedEmail(String toEmail, Instant startedAt, ZoneId recipientZoneId);
}
