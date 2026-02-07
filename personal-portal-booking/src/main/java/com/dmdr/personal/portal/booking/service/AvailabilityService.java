package com.dmdr.personal.portal.booking.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.dmdr.personal.portal.booking.dto.booking.BookingSuggestion;
import com.dmdr.personal.portal.booking.model.Booking;
import com.dmdr.personal.portal.booking.model.SessionType;

public interface AvailabilityService {
    void validateBookingAvailability(Instant startTime, Instant endTime);

    void validateBookingAvailabilityForUpdate(Booking updatingBooking, Instant requestedStartTime, Instant requestedEndTime);

    void validateBookingAvailabilityForAdmin(Instant startTime, Instant endTime);
    void validateBookingAvailabilityForAdminForUpdate(Booking updatingBooking, Instant startTime, Instant endTime);

    List<BookingSuggestion> calculateBookingSuggestion(SessionType sessionType, LocalDate suggestedDate,
                                                       Integer timezoneId);

    List<BookingSuggestion> calculateBookingSuggestionForUpdate(
            Booking bookingToUpdate,
            LocalDate suggestedDate,
            Integer timezoneId);
}
