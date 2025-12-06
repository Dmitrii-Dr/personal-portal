package com.dmdr.personal.portal.booking.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.dmdr.personal.portal.booking.dto.booking.BookingSuggestion;
import com.dmdr.personal.portal.booking.model.SessionType;

public interface AvailabilityService {
	void validateBookingAvailability(Instant startTime, Instant endTime);

	void validateBookingAvailabilityForAdmin(Instant startTime, Instant endTime);

	List<BookingSuggestion> calculateBookingSuggestion(SessionType sessionType, LocalDate suggestedDate, String timezone);
}

