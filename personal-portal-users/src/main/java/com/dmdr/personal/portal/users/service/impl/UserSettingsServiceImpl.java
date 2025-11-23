package com.dmdr.personal.portal.users.service.impl;

import com.dmdr.personal.portal.users.dto.CreateUserSettingsRequest;
import com.dmdr.personal.portal.users.dto.UpdateUserSettingsRequest;
import com.dmdr.personal.portal.users.dto.UserSettingsResponse;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.model.UserSettings;
import com.dmdr.personal.portal.users.repository.UserRepository;
import com.dmdr.personal.portal.users.repository.UserSettingsRepository;
import com.dmdr.personal.portal.users.service.UserSettingsService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSettingsServiceImpl implements UserSettingsService {

	private final UserSettingsRepository userSettingsRepository;
	private final UserRepository userRepository;

	public UserSettingsServiceImpl(
		UserSettingsRepository userSettingsRepository,
		UserRepository userRepository
	) {
		this.userSettingsRepository = userSettingsRepository;
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public UserSettingsResponse getSettings(UUID userId) {
		UserSettings settings = userSettingsRepository.findByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("User settings not found for user: " + userId));
		return UserSettingsResponse.from(settings);
	}

	@Override
	@Transactional
	public UserSettingsResponse createSettings(UUID userId, CreateUserSettingsRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

		// Check if settings already exist
		if (userSettingsRepository.findByUserId(userId).isPresent()) {
			throw new IllegalArgumentException("User settings already exist for user: " + userId);
		}

		UserSettings settings = new UserSettings();
		settings.setUser(user);
		settings.setTimezone(request.getTimezone());
		settings.setLanguage(request.getLanguage());

		UserSettings saved = userSettingsRepository.save(settings);
		return UserSettingsResponse.from(saved);
	}

	@Override
	@Transactional
	public UserSettingsResponse updateSettings(UUID userId, UpdateUserSettingsRequest request) {
		UserSettings settings = userSettingsRepository.findByUserId(userId)
			.orElseThrow(() -> new IllegalArgumentException("User settings not found for user: " + userId));

		settings.setTimezone(request.getTimezone());
		settings.setLanguage(request.getLanguage());

		UserSettings saved = userSettingsRepository.save(settings);
		return UserSettingsResponse.from(saved);
	}
}
