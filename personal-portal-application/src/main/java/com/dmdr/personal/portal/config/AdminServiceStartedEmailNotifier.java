package com.dmdr.personal.portal.config;

import com.dmdr.personal.portal.core.email.EmailService;
import com.dmdr.personal.portal.users.service.UserService;
import com.dmdr.personal.portal.users.service.UserSettingsService;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Notifies the configured support address by email when the application has finished starting.
 */
@Component
public class AdminServiceStartedEmailNotifier implements ApplicationListener<ApplicationReadyEvent> {

    private final EmailService emailService;
    private final UserService userService;
    private final UserSettingsService userSettingsService;
    private final String supportEmail;

    public AdminServiceStartedEmailNotifier(
            EmailService emailService,
            UserService userService,
            UserSettingsService userSettingsService,
            @Value("${admin.support.email}") String supportEmail) {
        this.emailService = emailService;
        this.userService = userService;
        this.userSettingsService = userSettingsService;
        this.supportEmail = supportEmail;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        ZoneId supportZoneId = userService.findByEmail(supportEmail)
                .map(user -> userSettingsService.getUserZoneIdOrDefault(user.getId()))
                .orElse(ZoneId.of("Europe/Moscow"));
        emailService.sendAdminServiceStartedEmail(supportEmail, Instant.now(), supportZoneId);
    }
}
