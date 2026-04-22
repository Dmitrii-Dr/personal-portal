package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.core.email.EmailService;
import com.dmdr.personal.portal.users.dto.CreateUserAdminRequest;
import com.dmdr.personal.portal.users.dto.CreateUserSettingsRequest;
import com.dmdr.personal.portal.users.dto.UserResponseForAdmin;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.service.UserService;
import com.dmdr.personal.portal.users.service.UserSettingsService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@Slf4j
public class AdminAuthController {

	private final UserService userService;
	private final UserSettingsService userSettingsService;
	private final EmailService emailService;

	public AdminAuthController(UserService userService, UserSettingsService userSettingsService,
			EmailService emailService) {
		this.userService = userService;
		this.userSettingsService = userSettingsService;
		this.emailService = emailService;
	}

	@PostMapping("/user/registry")
	public ResponseEntity<UserResponseForAdmin> registry(@Valid @RequestBody CreateUserAdminRequest request) {
		String ctx = "timezoneId=" + request.getTimezoneId()
			+ " emailNotifications=" + (request.getEmailNotificationEnabled() != null
				? request.getEmailNotificationEnabled()
				: "default");
		log.info("BEGIN adminUserRegistry {}", ctx);
		try {
		// Create user by admin
		User user = userService.createUserByAdmin(
				request.getEmail(),
				request.getFirstName(),
				request.getLastName(),
				request.getPhoneNumber());

		// Create user settings with timezone and emailNotificationEnabled
		CreateUserSettingsRequest settingsRequest = new CreateUserSettingsRequest();
		settingsRequest.setTimezoneId(request.getTimezoneId());
		settingsRequest.setEmailNotificationEnabled(
				request.getEmailNotificationEnabled() != null ? request.getEmailNotificationEnabled() : true);
		userSettingsService.createSettings(user.getId(), settingsRequest);

		// Send welcome email if email notifications are enabled (check UserSettings)
		if (userSettingsService.isEmailNotificationEnabled(user.getId())) {
			try {
				emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName(), user.getLastName());
			} catch (Exception e) {
				// Log error but don't fail user creation if email fails
				log.error("Failed to send welcome email after admin user registry", e);
			}
		}

		return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseForAdmin.from(user));
		} finally {
			log.info("END adminUserRegistry {}", ctx);
		}
	}
}
