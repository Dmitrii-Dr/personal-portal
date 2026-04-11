package com.dmdr.personal.portal.config;

import com.dmdr.personal.portal.core.email.EmailService;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Notifies the configured support address by email when the application has finished starting.
 */
@Component
public class AdminServiceStartedEmailNotifier implements ApplicationListener<ApplicationReadyEvent> {

    private final EmailService emailService;
    private final String supportEmail;

    public AdminServiceStartedEmailNotifier(
            EmailService emailService,
            @Value("${admin.support.email}") String supportEmail) {
        this.emailService = emailService;
        this.supportEmail = supportEmail;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        emailService.sendAdminServiceStartedEmail(supportEmail, Instant.now());
    }
}
