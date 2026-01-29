package com.dmdr.personal.portal.users.dto;

import com.dmdr.personal.portal.core.model.TimezoneEntry;
import com.dmdr.personal.portal.users.model.UserSettings;

public record UserSettingsResponse(
		Long id,
		TimezoneEntry timezone,
		String language,
		String currency,
		boolean emailNotificationEnabled) {
	public static UserSettingsResponse from(UserSettings settings) {
		return new UserSettingsResponse(
				settings.getId(),
				settings.getTimezoneId() != null ? TimezoneEntry.getById(settings.getTimezoneId()) : null,
				settings.getLanguage(),
				settings.getCurrency().getDisplayName(),
				settings.isEmailNotificationEnabled());
	}
}
