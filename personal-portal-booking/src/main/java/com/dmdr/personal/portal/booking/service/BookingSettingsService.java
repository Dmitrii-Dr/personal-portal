package com.dmdr.personal.portal.booking.service;

import com.dmdr.personal.portal.booking.dto.booking.AdminBookingSettingsResponse;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingSettingsRequest;
import com.dmdr.personal.portal.core.model.TimezoneEntry;

public interface BookingSettingsService {
	AdminBookingSettingsResponse getSettings();

	AdminBookingSettingsResponse updateSettings(UpdateBookingSettingsRequest request);

	TimezoneEntry getDefaultTimezone();
}
