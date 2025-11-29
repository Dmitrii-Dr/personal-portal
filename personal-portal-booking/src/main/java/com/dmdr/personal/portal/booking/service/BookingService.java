package com.dmdr.personal.portal.booking.service;

import com.dmdr.personal.portal.booking.dto.booking.BookingResponse;
import com.dmdr.personal.portal.booking.dto.booking.CreateBookingRequest;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingRequest;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingStatusRequest;
import com.dmdr.personal.portal.booking.model.BookingStatus;

import java.util.List;
import java.util.UUID;

public interface BookingService {
	List<BookingResponse> getAllForUser(UUID userId);
	BookingResponse create(UUID userId, CreateBookingRequest request);
	BookingResponse update(UUID userId, UpdateBookingRequest request);
	void delete(UUID userId, Long bookingId);
	List<BookingResponse> getAllByStatus(BookingStatus status);
	List<com.dmdr.personal.portal.booking.model.Booking> getAllBookingsByStatus(BookingStatus status);
	BookingResponse updateStatus(UpdateBookingStatusRequest request);
}

