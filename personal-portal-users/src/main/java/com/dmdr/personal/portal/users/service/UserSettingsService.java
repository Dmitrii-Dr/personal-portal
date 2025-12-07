package com.dmdr.personal.portal.users.service;

import com.dmdr.personal.portal.core.model.Currency;
import com.dmdr.personal.portal.users.dto.CreateUserSettingsRequest;
import com.dmdr.personal.portal.users.dto.UpdateUserSettingsRequest;
import com.dmdr.personal.portal.users.dto.UserSettingsResponse;
import java.util.UUID;

public interface UserSettingsService {
	UserSettingsResponse getSettings(UUID userId);
	UserSettingsResponse createSettings(UUID userId, CreateUserSettingsRequest request);
	UserSettingsResponse updateSettings(UUID userId, UpdateUserSettingsRequest request);
	Currency getUserCurrency(UUID userId);
	boolean isEmailNotificationEnabled(UUID userId);
}
