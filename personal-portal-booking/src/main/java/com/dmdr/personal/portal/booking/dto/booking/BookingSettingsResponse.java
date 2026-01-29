package com.dmdr.personal.portal.booking.dto.booking;

import com.dmdr.personal.portal.core.model.TimezoneEntry;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingSettingsResponse {
	private Long id;
	private int bookingSlotsInterval;
	private int bookingFirstSlotInterval;
	private int bookingCancelationInterval;
	private int bookingUpdatingInterval;
	private TimezoneEntry defaultTimezone;
	private String defaultUtcOffset;
}
