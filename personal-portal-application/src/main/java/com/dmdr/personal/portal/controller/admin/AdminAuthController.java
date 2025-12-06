package com.dmdr.personal.portal.controller.admin;

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

	public AdminAuthController(UserService userService, UserSettingsService userSettingsService) {
		this.userService = userService;
		this.userSettingsService = userSettingsService;
	}

	@PostMapping("/user/registry")
	public ResponseEntity<UserResponse> registry(@Valid @RequestBody CreateUserAdminRequest request) {
		// Create user by admin
		com.dmdr.personal.portal.users.model.User user = userService.createUserByAdmin(
			request.getEmail(),
			request.getFirstName(),
			request.getLastName(),
			request.isSendEmailNotification()
		);

		// Create user settings with timezone (use default language "en")
		CreateUserSettingsRequest settingsRequest = new CreateUserSettingsRequest();
		settingsRequest.setTimezone(request.getTimezone());
		settingsRequest.setLanguage("en");  // Default language
		userSettingsService.createSettings(user.getId(), settingsRequest);

		return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
	}
}

