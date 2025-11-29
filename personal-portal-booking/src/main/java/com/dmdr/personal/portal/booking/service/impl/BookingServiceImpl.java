package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.booking.BookingResponse;
import com.dmdr.personal.portal.booking.dto.booking.CreateBookingRequest;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingRequest;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingStatusRequest;
import com.dmdr.personal.portal.core.email.EmailService;
import com.dmdr.personal.portal.core.security.SystemRole;
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
	private final EmailService emailService;
	private final com.dmdr.personal.portal.users.service.UserService userService;

	public BookingServiceImpl(
		BookingRepository bookingRepository,
		SessionTypeRepository sessionTypeRepository,
		UserRepository userRepository,
		BookingSettingsRepository bookingSettingsRepository,
		AvailabilityService availabilityService,
		EmailService emailService,
		com.dmdr.personal.portal.users.service.UserService userService
	) {
		this.bookingRepository = bookingRepository;
		this.sessionTypeRepository = sessionTypeRepository;
		this.userRepository = userRepository;
		this.bookingSettingsRepository = bookingSettingsRepository;
		this.availabilityService = availabilityService;
		this.emailService = emailService;
		this.userService = userService;
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
		
		// Send email notification to all admin users
		try {
			List<User> adminUsers = userService.findByRoleName(SystemRole.ADMIN.getAuthority(), null);
			String clientName = (client.getFirstName() != null ? client.getFirstName() : "") 
				+ (client.getLastName() != null ? " " + client.getLastName() : "").trim();
			if (clientName.isEmpty()) {
				clientName = "Unknown User";
			}
			
			for (User admin : adminUsers) {
				try {
					emailService.sendBookingRequestAdminEmail(
						admin.getEmail(),
						clientName,
						client.getEmail(),
						sessionType.getName(),
						saved.getStartTime(),
						saved.getClientMessage()
					);
				} catch (Exception e) {
					System.err.println("Failed to send booking request email to admin " + admin.getEmail() + ": " + e.getMessage());
				}
			}
		} catch (Exception e) {
			System.err.println("Failed to send booking request notifications to admins: " + e.getMessage());
		}
		
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

	@Override
	@Transactional(readOnly = true)
	public List<BookingResponse> getAllByStatus(BookingStatus status) {
		return bookingRepository.findByStatusOrderByStartTimeAsc(status).stream()
			.map(BookingServiceImpl::toResponse)
			.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<Booking> getAllBookingsByStatus(BookingStatus status) {
		return bookingRepository.findByStatusOrderByStartTimeAsc(status);
	}

	@Override
	@Transactional
	public BookingResponse updateStatus(UpdateBookingStatusRequest request) {
		Booking booking = bookingRepository.findById(request.getId())
			.orElseThrow(() -> new IllegalArgumentException("Booking not found: " + request.getId()));

		BookingStatus currentStatus = booking.getStatus();
		BookingStatus newStatus = request.getStatus();

		// Validate status transition
		validateStatusTransition(currentStatus, newStatus);

		// Update status
		booking.setStatus(newStatus);
		Booking saved = bookingRepository.save(booking);

		// Send email notifications for CONFIRMED or DECLINED
		User client = booking.getClient();
		if (newStatus == BookingStatus.CONFIRMED) {
			try {
				emailService.sendBookingConfirmationEmail(
					client.getEmail(),
					client.getFirstName(),
					client.getLastName(),
					booking.getSessionType().getName(),
					booking.getStartTime()
				);
			} catch (Exception e) {
				System.err.println("Failed to send booking confirmation email: " + e.getMessage());
			}
		} else if (newStatus == BookingStatus.DECLINED) {
			try {
				emailService.sendBookingRejectionEmail(
					client.getEmail(),
					client.getFirstName(),
					client.getLastName(),
					booking.getSessionType().getName(),
					booking.getStartTime()
				);
			} catch (Exception e) {
				System.err.println("Failed to send booking rejection email: " + e.getMessage());
			}
		}

		return toResponse(saved);
	}

	private void validateStatusTransition(BookingStatus currentStatus, BookingStatus newStatus) {
		if (currentStatus == newStatus) {
			return; // No change, allow it
		}

		boolean isValid = switch (currentStatus) {
			case PENDING_APPROVAL -> newStatus == BookingStatus.CONFIRMED 
				|| newStatus == BookingStatus.DECLINED 
				|| newStatus == BookingStatus.CANCELLED;
			case CONFIRMED -> newStatus == BookingStatus.DECLINED 
				|| newStatus == BookingStatus.CANCELLED 
				|| newStatus == BookingStatus.COMPLETED;
			default -> false;
		};

		if (!isValid) {
			throw new IllegalArgumentException(
				"Invalid status transition from " + currentStatus + " to " + newStatus
			);
		}
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

