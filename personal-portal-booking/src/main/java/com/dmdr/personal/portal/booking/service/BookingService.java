package com.dmdr.personal.portal.booking.service;

import com.dmdr.personal.portal.booking.dto.booking.AdminBookingsGroupedByStatusResponse;
import com.dmdr.personal.portal.booking.dto.booking.BookingResponse;
import com.dmdr.personal.portal.booking.dto.booking.BookingsGroupedByStatusResponse;
import com.dmdr.personal.portal.booking.dto.booking.CreateBookingRequest;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingRequest;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingStatusRequest;
import com.dmdr.personal.portal.booking.model.BookingStatus;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingService {
	List<BookingResponse> getAllForUser(UUID userId);
	List<BookingResponse> getBookingsByStatusesForUser(UUID userId, Set<BookingStatus> statuses);
	BookingsGroupedByStatusResponse getBookingsGroupedByStatusForUser(UUID userId, Set<BookingStatus> statuses);
	BookingResponse create(UUID userId, CreateBookingRequest request);
	BookingResponse update(UUID userId, UpdateBookingRequest request);
	BookingResponse cancel(UUID userId, Long bookingId);
	void delete(UUID userId, Long bookingId);
	List<BookingResponse> getAllByStatus(BookingStatus status);
	List<com.dmdr.personal.portal.booking.model.Booking> getAllBookingsByStatus(BookingStatus status);
	Page<com.dmdr.personal.portal.booking.model.Booking> getAllBookingsByStatus(BookingStatus status, Pageable pageable);
	AdminBookingsGroupedByStatusResponse getBookingsGroupedByStatus(Set<BookingStatus> statuses);
	BookingResponse updateStatus(UpdateBookingStatusRequest request);
}

