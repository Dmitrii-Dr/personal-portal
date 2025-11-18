package com.dmdr.personal.portal.booking.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.dmdr.personal.portal.booking.dto.booking.BookingSuggestion;

public interface AvailabilityService {
	void validateBookingAvailability(Instant startTime, Instant endTime);

	List<BookingSuggestion> calculateBookingSuggestion(int sessionDurationMinutes, LocalDate suggestedDate, String timezone);
}

