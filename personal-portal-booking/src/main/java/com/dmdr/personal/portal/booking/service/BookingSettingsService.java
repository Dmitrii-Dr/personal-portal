package com.dmdr.personal.portal.booking.service;

import com.dmdr.personal.portal.booking.dto.booking.BookingSettingsResponse;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingSettingsRequest;
import com.dmdr.personal.portal.core.model.TimezoneEntry;

public interface BookingSettingsService {
	BookingSettingsResponse getSettings();

	BookingSettingsResponse updateSettings(UpdateBookingSettingsRequest request);

	TimezoneEntry getDefaultTimezone();
}
