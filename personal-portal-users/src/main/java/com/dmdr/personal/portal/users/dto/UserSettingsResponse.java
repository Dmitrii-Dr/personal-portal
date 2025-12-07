package com.dmdr.personal.portal.users.dto;

import com.dmdr.personal.portal.users.model.UserSettings;

public record UserSettingsResponse(
	Long id,
	String timezone,
	String language,
	String currency,
	boolean emailNotificationEnabled
) {
	public static UserSettingsResponse from(UserSettings settings) {
		return new UserSettingsResponse(
			settings.getId(),
			settings.getTimezone(),
			settings.getLanguage(),
			settings.getCurrency().getDisplayName(),
			settings.isEmailNotificationEnabled()
		);
	}
}
