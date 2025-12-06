package com.dmdr.personal.portal.users.service.impl;

import com.dmdr.personal.portal.core.model.Currency;
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

	/**
	 * Get user's currency preference. Creates UserSettings with default currency if they don't exist.
	 */
	@Override
	@Transactional
	public Currency getUserCurrency(UUID userId) {
		UserSettings settings = userSettingsRepository.findByUserId(userId).orElse(null);
		if (settings == null) {
			// Create UserSettings with default currency on first access
			User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
			settings = new UserSettings();
			settings.setUser(user);
			// Set default values - these should ideally come from a configuration
			// For now, using reasonable defaults
			settings.setTimezone("UTC");
			settings.setLanguage("en");
			settings.setCurrency(Currency.RUB);
			settings = userSettingsRepository.save(settings);
		}
		return settings.getCurrency();
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
		settings.setCurrency(request.getCurrency() != null ? request.getCurrency() : Currency.RUB);

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
		if (request.getCurrency() != null) {
			settings.setCurrency(request.getCurrency());
		}

		UserSettings saved = userSettingsRepository.save(settings);
		return UserSettingsResponse.from(saved);
	}
}
