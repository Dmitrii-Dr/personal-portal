package com.dmdr.personal.portal.booking.dto.booking;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingSettingsResponse {
	private int bookingCancelationInterval;
	private int bookingUpdatingInterval;
}
