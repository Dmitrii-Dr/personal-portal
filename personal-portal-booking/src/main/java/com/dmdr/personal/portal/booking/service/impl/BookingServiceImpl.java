package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.booking.BookingResponse;
import com.dmdr.personal.portal.booking.dto.booking.CreateBookingRequest;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingRequest;
import com.dmdr.personal.portal.booking.model.Booking;
import com.dmdr.personal.portal.booking.model.BookingSettings;
import com.dmdr.personal.portal.booking.model.BookingStatus;
import com.dmdr.personal.portal.booking.model.SessionType;
import com.dmdr.personal.portal.booking.repository.BookingRepository;
import com.dmdr.personal.portal.booking.repository.BookingSettingsRepository;
import com.dmdr.personal.portal.booking.repository.SessionTypeRepository;
import com.dmdr.personal.portal.booking.service.AvailabilityService;
import com.dmdr.personal.portal.booking.service.BookingService;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingServiceImpl implements BookingService {

	private final BookingRepository bookingRepository;
	private final SessionTypeRepository sessionTypeRepository;
	private final UserRepository userRepository;
	private final BookingSettingsRepository bookingSettingsRepository;
	private final AvailabilityService availabilityService;

	public BookingServiceImpl(
		BookingRepository bookingRepository,
		SessionTypeRepository sessionTypeRepository,
		UserRepository userRepository,
		BookingSettingsRepository bookingSettingsRepository,
		AvailabilityService availabilityService
	) {
		this.bookingRepository = bookingRepository;
		this.sessionTypeRepository = sessionTypeRepository;
		this.userRepository = userRepository;
		this.bookingSettingsRepository = bookingSettingsRepository;
		this.availabilityService = availabilityService;
	}

	@Override
	@Transactional(readOnly = true)
	public List<BookingResponse> getAllForUser(UUID userId) {
		return bookingRepository.findByClientId(userId).stream()
			.map(BookingServiceImpl::toResponse)
			.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public BookingResponse create(UUID userId, CreateBookingRequest request) {
		User client = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
		SessionType sessionType = sessionTypeRepository.findById(request.getSessionTypeId())
			.orElseThrow(() -> new IllegalArgumentException("SessionType not found: " + request.getSessionTypeId()));

		// Calculate endTime: startTime + duration + buffer
		Instant endTime = request.getStartTimeInstant().plusSeconds(
			(sessionType.getDurationMinutes() + sessionType.getBufferMinutes()) * 60L
		);

		//TODO enhance validation logic with checking overrides and already booked sesssions 
		// Validate booking availability (check that startTime and endTime are within available hours)
		availabilityService.validateBookingAvailability(request.getStartTimeInstant(), endTime);

		Booking entity = new Booking();
		entity.setClient(client);
		entity.setSessionType(sessionType);
		entity.setStartTime(request.getStartTimeInstant());
		// Set endTime (includes duration + buffer for validation purposes)
		entity.setEndTime(endTime);
		entity.setStatus(BookingStatus.PENDING_APPROVAL);
		entity.setClientMessage(request.getClientMessage());

		Booking saved = bookingRepository.save(entity);
		return toResponse(saved);
	}

	@Override
	@Transactional
	public BookingResponse update(UUID userId, UpdateBookingRequest request) {
		Booking entity = bookingRepository.findById(request.getId())
			.orElseThrow(() -> new IllegalArgumentException("Booking not found: " + request.getId()));

		// Verify the booking belongs to the user
		if (!entity.getClient().getId().equals(userId)) {
			throw new IllegalArgumentException("Booking does not belong to user");
		}

		// Validate booking updating interval
		BookingSettings settings = getBookingSettings();
		Instant now = Instant.now();
		Duration timeUntilBooking = Duration.between(now, entity.getStartTime());
		long minutesUntilBooking = timeUntilBooking.toMinutes();
		
		if (minutesUntilBooking < settings.getBookingUpdatingInterval()) {
			throw new IllegalArgumentException(
				"Booking can only be updated at least " + settings.getBookingUpdatingInterval() + " minutes before the start time");
		}

		entity.setStartTime(request.getStartTime());
		// Recalculate endTime based on session duration
		SessionType sessionType = entity.getSessionType();
		Instant endTime = request.getStartTime().plusSeconds(sessionType.getDurationMinutes() * 60L);
		entity.setEndTime(endTime);
		entity.setClientMessage(request.getClientMessage());
		entity.setStatus(BookingStatus.PENDING_APPROVAL);

		Booking saved = bookingRepository.save(entity);
		return toResponse(saved);
	}

	@Override
	@Transactional
	public void delete(UUID userId, Long bookingId) {
		Booking entity = bookingRepository.findById(bookingId)
			.orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

		// Verify the booking belongs to the user
		if (!entity.getClient().getId().equals(userId)) {
			throw new IllegalArgumentException("Booking does not belong to user: " + userId);
		}

		// Validate booking cancellation interval
		BookingSettings settings = getBookingSettings();
		Instant now = Instant.now();
		Duration timeUntilBooking = Duration.between(now, entity.getStartTime());
		long minutesUntilBooking = timeUntilBooking.toMinutes();
		
		if (minutesUntilBooking < settings.getBookingCancelationInterval()) {
			throw new IllegalArgumentException(
				"Booking can only be cancelled at least " + settings.getBookingCancelationInterval() + " minutes before the start time");
		}

		bookingRepository.delete(entity);
	}

	private BookingSettings getBookingSettings() {
		return bookingSettingsRepository.mustFindTopByOrderByIdAsc();
	}

	private static BookingResponse toResponse(Booking entity) {
		BookingResponse resp = new BookingResponse();
		resp.setId(entity.getId());
		resp.setSessionTypeId(entity.getSessionType().getId());
		resp.setSessionTypeName(entity.getSessionType().getName());
		resp.setStartTimeInstant(entity.getStartTime());
		resp.setEndTimeInstant(entity.getEndTime());
		resp.setStatus(entity.getStatus());
		resp.setClientMessage(entity.getClientMessage());
		resp.setCreatedAt(entity.getCreatedAt());
		return resp;
	}
}

