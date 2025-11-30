package com.dmdr.personal.portal.booking.dto.booking;

import com.dmdr.personal.portal.booking.model.BookingStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminBookingsGroupedByStatusResponse {
	private Map<BookingStatus, List<AdminBookingResponse>> bookings;

	public AdminBookingsGroupedByStatusResponse() {
		this.bookings = new HashMap<>();
	}

	public void addBookingsForStatus(BookingStatus status, List<AdminBookingResponse> bookings) {
		this.bookings.put(status, bookings);
	}
}

