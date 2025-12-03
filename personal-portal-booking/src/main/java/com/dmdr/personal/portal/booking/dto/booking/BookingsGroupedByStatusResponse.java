package com.dmdr.personal.portal.booking.dto.booking;

import com.dmdr.personal.portal.booking.model.BookingStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingsGroupedByStatusResponse {
	private Map<BookingStatus, List<BookingResponse>> bookings;

	public BookingsGroupedByStatusResponse() {
		this.bookings = new HashMap<>();
	}

	public void addBookingsForStatus(BookingStatus status, List<BookingResponse> bookings) {
		this.bookings.put(status, bookings);
	}
}

