package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.core.email.EmailService;
import com.dmdr.personal.portal.users.dto.CreateUserAdminRequest;
import com.dmdr.personal.portal.users.dto.CreateUserSettingsRequest;
import com.dmdr.personal.portal.users.dto.UserResponse;
import com.dmdr.personal.portal.users.service.UserService;
import com.dmdr.personal.portal.users.service.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminAuthController {

	private final UserService userService;
	private final UserSettingsService userSettingsService;
	private final EmailService emailService;

	public AdminAuthController(UserService userService, UserSettingsService userSettingsService, EmailService emailService) {
		this.userService = userService;
		this.userSettingsService = userSettingsService;
		this.emailService = emailService;
	}

	@PostMapping("/user/registry")
	public ResponseEntity<UserResponse> registry(@Valid @RequestBody CreateUserAdminRequest request) {
		// Create user by admin
		com.dmdr.personal.portal.users.model.User user = userService.createUserByAdmin(
			request.getEmail(),
			request.getFirstName(),
			request.getLastName()
		);

		// Create user settings with timezone and emailNotificationEnabled
		CreateUserSettingsRequest settingsRequest = new CreateUserSettingsRequest();
		settingsRequest.setTimezone(request.getTimezone());
		settingsRequest.setLanguage("en");  // Default language
		settingsRequest.setEmailNotificationEnabled(request.getEmailNotificationEnabled() != null ? request.getEmailNotificationEnabled() : true);
		userSettingsService.createSettings(user.getId(), settingsRequest);

		// Send welcome email if email notifications are enabled (check UserSettings)
		if (userSettingsService.isEmailNotificationEnabled(user.getId())) {
			try {
				emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName(), user.getLastName());
			} catch (Exception e) {
				// Log error but don't fail user creation if email fails
				System.err.println("Failed to send welcome email to " + user.getEmail() + ": " + e.getMessage());
			}
		}

		return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
	}
}

