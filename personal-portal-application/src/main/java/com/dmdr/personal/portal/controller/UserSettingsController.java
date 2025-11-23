package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.service.CurrentUserService;
import com.dmdr.personal.portal.users.dto.CreateUserSettingsRequest;
import com.dmdr.personal.portal.users.dto.UpdateUserSettingsRequest;
import com.dmdr.personal.portal.users.dto.UserSettingsResponse;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.service.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/setting")
public class UserSettingsController {

	private final UserSettingsService userSettingsService;
	private final CurrentUserService currentUserService;

	public UserSettingsController(
		UserSettingsService userSettingsService,
		CurrentUserService currentUserService
	) {
		this.userSettingsService = userSettingsService;
		this.currentUserService = currentUserService;
	}

	@GetMapping
	public ResponseEntity<UserSettingsResponse> getSettings() {
		User currentUser = currentUserService.getCurrentUser();
		UserSettingsResponse response = userSettingsService.getSettings(currentUser.getId());
		return ResponseEntity.ok(response);
	}

	@PostMapping
	public ResponseEntity<UserSettingsResponse> createSettings(
		@Valid @RequestBody CreateUserSettingsRequest request
	) {
		User currentUser = currentUserService.getCurrentUser();
		UserSettingsResponse response = userSettingsService.createSettings(currentUser.getId(), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping
	public ResponseEntity<UserSettingsResponse> updateSettings(
		@Valid @RequestBody UpdateUserSettingsRequest request
	) {
		User currentUser = currentUserService.getCurrentUser();
		UserSettingsResponse response = userSettingsService.updateSettings(currentUser.getId(), request);
		return ResponseEntity.ok(response);
	}
}
