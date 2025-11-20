package com.dmdr.personal.portal.booking.dto.booking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBookingSettingsRequest {

	@Min(0)
	@NotNull
	private int bookingSlotsInterval;

	@Min(10)
	@NotNull
	private int bookingFirstSlotInterval;

	@Min(0)
	@NotNull
	private int bookingCancelationInterval;

	@Min(0)
	@NotNull
	private int bookingUpdatingInterval;

	@NotNull
	private String defaultTimezone;
}


