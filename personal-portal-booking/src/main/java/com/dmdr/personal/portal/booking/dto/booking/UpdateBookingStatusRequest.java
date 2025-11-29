package com.dmdr.personal.portal.booking.dto.booking;

import com.dmdr.personal.portal.booking.model.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateBookingStatusRequest {
	@NotNull(message = "Booking id is required")
	private Long id;

	@NotNull(message = "Status is required")
	private BookingStatus status;
}

