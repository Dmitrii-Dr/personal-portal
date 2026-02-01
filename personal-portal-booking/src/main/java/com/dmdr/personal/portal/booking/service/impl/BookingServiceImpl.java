package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.booking.AdminBookingResponse;
import com.dmdr.personal.portal.booking.dto.booking.BookingResponse;
import com.dmdr.personal.portal.booking.dto.booking.AdminBookingsGroupedByStatusResponse;
import com.dmdr.personal.portal.booking.dto.booking.BookingsGroupedByStatusResponse;
import com.dmdr.personal.portal.booking.dto.booking.CreateBookingAdminRequest;
import com.dmdr.personal.portal.booking.dto.booking.CreateBookingRequest;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingAdminRequest;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingRequest;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingStatusRequest;
import com.dmdr.personal.portal.core.email.EmailService;
import com.dmdr.personal.portal.core.model.Currency;
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
import com.dmdr.personal.portal.users.service.UserSettingsService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingServiceImpl implements BookingService {
	private final Object bookingLock = new Object();

	private final BookingRepository bookingRepository;
	private final SessionTypeRepository sessionTypeRepository;
	private final UserRepository userRepository;
	private final BookingSettingsRepository bookingSettingsRepository;
	private final AvailabilityService availabilityService;
	private final EmailService emailService;
	private final com.dmdr.personal.portal.users.service.UserService userService;
	private final UserSettingsService userSettingsService;

	public BookingServiceImpl(
		BookingRepository bookingRepository,
		SessionTypeRepository sessionTypeRepository,
		UserRepository userRepository,
		BookingSettingsRepository bookingSettingsRepository,
		AvailabilityService availabilityService,
		EmailService emailService,
		com.dmdr.personal.portal.users.service.UserService userService,
		UserSettingsService userSettingsService
	) {
		this.bookingRepository = bookingRepository;
		this.sessionTypeRepository = sessionTypeRepository;
		this.userRepository = userRepository;
		this.bookingSettingsRepository = bookingSettingsRepository;
		this.availabilityService = availabilityService;
		this.emailService = emailService;
		this.userService = userService;
		this.userSettingsService = userSettingsService;
	}

	@Override
	@Transactional(readOnly = true)
	public List<BookingResponse> getAllForUser(UUID userId) {
		return bookingRepository.findByClientId(userId).stream()
			.map(BookingServiceImpl::toResponse)
			.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<BookingResponse> getBookingsByStatusesForUser(UUID userId, Set<BookingStatus> statuses) {
		if (statuses == null || statuses.isEmpty()) {
			return List.of();
		}

		return bookingRepository.findByClientIdAndStatusInOrderByStartTimeAsc(userId, statuses).stream()
			.map(BookingServiceImpl::toResponse)
			.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public BookingsGroupedByStatusResponse getBookingsGroupedByStatusForUser(UUID userId, Set<BookingStatus> statuses) {
		if (statuses == null || statuses.isEmpty()) {
			return new BookingsGroupedByStatusResponse();
		}

		List<Booking> bookings = bookingRepository.findByClientIdAndStatusInOrderByStartTimeAsc(userId, statuses);
		
		// Group bookings by status
		Map<BookingStatus, List<Booking>> bookingsByStatus = bookings.stream()
			.collect(Collectors.groupingBy(Booking::getStatus));

		// Convert to response DTO
		BookingsGroupedByStatusResponse response = new BookingsGroupedByStatusResponse();
		for (BookingStatus status : statuses) {
			List<Booking> statusBookings = bookingsByStatus.getOrDefault(status, List.of());
			List<BookingResponse> bookingResponses = statusBookings.stream()
				.map(BookingServiceImpl::toResponse)
				.sorted(Comparator.comparing(BookingResponse::getStartTimeInstant))
				.collect(Collectors.toList());
			response.addBookingsForStatus(status, bookingResponses);
		}

		return response;
	}

	@Override
	@Transactional
	public BookingResponse create(UUID userId, CreateBookingRequest request) {
		User client = userRepository.findById(userId)
			.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
		SessionType sessionType = sessionTypeRepository.findById(request.getSessionTypeId())
			.orElseThrow(() -> new IllegalArgumentException("SessionType not found: " + request.getSessionTypeId()));

		if (!sessionType.isActive()) {
			throw new IllegalArgumentException("Cannot create booking with inactive session type");
		}

		// Calculate endTime: startTime + duration + buffer
		Instant endTime = request.getStartTimeInstant().plusSeconds(
			(sessionType.getDurationMinutes() + sessionType.getBufferMinutes()) * 60L
		);

		Booking saved = null;
		synchronized (bookingLock) {
			availabilityService.validateBookingAvailability(request.getStartTimeInstant(), endTime);

			Booking entity = new Booking();
			entity.setClient(client);
			// Copy session type data into booking (denormalization)
			entity.setSessionName(sessionType.getName());
			entity.setSessionDurationMinutes(sessionType.getDurationMinutes());
			entity.setSessionBufferMinutes(sessionType.getBufferMinutes());
			// Copy session type prices filtered by user's currency
			Currency userCurrency = userSettingsService.getUserCurrency(userId);
			Map<String, BigDecimal> filteredPrices = filterPricesByCurrency(sessionType.getPrices(), userCurrency);
			entity.setSessionPrices(filteredPrices);
			entity.setSessionDescription(sessionType.getDescription());
			entity.setStartTime(request.getStartTimeInstant());
			// Set endTime (includes duration + buffer for validation purposes)
			entity.setEndTime(endTime);
			entity.setStatus(BookingStatus.PENDING_APPROVAL);
			entity.setClientMessage(request.getClientMessage());

			saved = bookingRepository.saveAndFlush(entity);
		}

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
	public AdminBookingResponse createByAdmin(CreateBookingAdminRequest request) {

		// Validate session type
		SessionType sessionType = sessionTypeRepository.findById(request.getSessionTypeId())
			.orElseThrow(() -> new IllegalArgumentException("SessionType not found: " + request.getSessionTypeId()));

		if (!sessionType.isActive()) {
			throw new IllegalArgumentException("Cannot create booking with inactive session type");
		}

		// Find user by userId (required)
		User client = userRepository.findById(request.getUserId())
			.orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));

		// Calculate endTime: startTime + duration + buffer
		Instant endTime = request.getStartTimeInstant().plusSeconds(
			(sessionType.getDurationMinutes() + sessionType.getBufferMinutes()) * 60L
		);
		Booking saved = null;

		synchronized (bookingLock) {
			// Validate availability using admin validation (no rules/overrides check)
			availabilityService.validateBookingAvailabilityForAdmin(request.getStartTimeInstant(), endTime);

			// Create booking entity
			Booking entity = new Booking();
			entity.setClient(client);
			// Copy session type data into booking (denormalization)
			entity.setSessionName(sessionType.getName());
			entity.setSessionDurationMinutes(sessionType.getDurationMinutes());
			entity.setSessionBufferMinutes(sessionType.getBufferMinutes());
			// Copy session type prices filtered by user's currency
			Currency userCurrency = userSettingsService.getUserCurrency(request.getUserId());
			Map<String, BigDecimal> filteredPrices = filterPricesByCurrency(sessionType.getPrices(), userCurrency);
			entity.setSessionPrices(filteredPrices);
			entity.setSessionDescription(sessionType.getDescription());
			entity.setStartTime(request.getStartTimeInstant());
			// Set endTime (includes duration + buffer for validation purposes)
			entity.setEndTime(endTime);
			entity.setStatus(BookingStatus.PENDING_APPROVAL);
			entity.setClientMessage(request.getClientMessage());

			saved = bookingRepository.save(entity);
		}

		return toAdminResponse(saved);
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

		// Update is allowed only when Status is PENDING_APPROVAL
		if (entity.getStatus() != BookingStatus.PENDING_APPROVAL) {
			throw new IllegalArgumentException("Booking can only be updated when status is PENDING_APPROVAL. Current status: " + entity.getStatus());
		}

		// Validate that new startTime is not in the past
		Instant now = Instant.now();
		if (request.getStartTime().isBefore(now) || request.getStartTime().equals(now)) {
			throw new IllegalArgumentException("Start time must be in the future");
		}

		// Validate booking updating interval
		BookingSettings settings = getBookingSettings();
		Duration timeUntilBooking = Duration.between(now, entity.getStartTime());
		long minutesUntilBooking = timeUntilBooking.toMinutes();
		
		if (minutesUntilBooking < settings.getBookingUpdatingInterval()) {
			throw new IllegalArgumentException(
				"Booking can only be updated at least " + settings.getBookingUpdatingInterval() + " minutes before the start time");
		}

		// Recalculate endTime based on session duration + buffer (for validation)
		// Use denormalized session data from booking
		Instant endTime = request.getStartTime().plusSeconds(
			(entity.getSessionDurationMinutes() + entity.getSessionBufferMinutes()) * 60L
		);
		Booking saved = null;
		synchronized (bookingLock) {
			// Validate booking availability for the new time slot
			availabilityService.validateBookingAvailability(request.getStartTime(), endTime);

			entity.setStartTime(request.getStartTime());
			// Set endTime (includes duration only, buffer is for validation purposes)
			Instant endTimeForEntity = request.getStartTime().plusSeconds(entity.getSessionDurationMinutes() * 60L);
			entity.setEndTime(endTimeForEntity);
			entity.setClientMessage(request.getClientMessage());
			entity.setStatus(BookingStatus.PENDING_APPROVAL);

			saved = bookingRepository.saveAndFlush(entity);
		}

		return toResponse(saved);
	}

	@Override
	@Transactional
	public AdminBookingResponse updateByAdmin(UpdateBookingAdminRequest request) {
		Booking entity = bookingRepository.findById(request.getId())
			.orElseThrow(() -> new IllegalArgumentException("Booking not found: " + request.getId()));

		// Validate booking status is CONFIRMED or PENDING_APPROVAL
		BookingStatus currentStatus = entity.getStatus();
		if (currentStatus != BookingStatus.CONFIRMED && currentStatus != BookingStatus.PENDING_APPROVAL) {
			throw new IllegalArgumentException(
				"Booking can only be updated when status is CONFIRMED or PENDING_APPROVAL. Current status: " + currentStatus);
		}

		// Find user by userId (required)
		User client = userRepository.findById(request.getUserId())
			.orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));

		// Recalculate endTime based on session duration + buffer (for validation)
		// Use denormalized session data from booking
		Instant endTime = request.getStartTime().plusSeconds(
			(entity.getSessionDurationMinutes() + entity.getSessionBufferMinutes()) * 60L
		);
		Booking saved = null;
		synchronized (bookingLock) {
			// Validate booking availability using admin validation (no rules/overrides check)
			availabilityService.validateBookingAvailabilityForAdmin(request.getStartTime(), endTime);

			// Update booking entity
			entity.setClient(client);
			entity.setStartTime(request.getStartTime());
			// Set endTime (includes duration only, buffer is for validation purposes)
			Instant endTimeForEntity = request.getStartTime().plusSeconds(entity.getSessionDurationMinutes() * 60L);
			entity.setEndTime(endTimeForEntity);
			entity.setClientMessage(request.getClientMessage());
			// Keep the existing status (CONFIRMED or PENDING_APPROVAL)

			saved = bookingRepository.saveAndFlush(entity);
		}

		return toAdminResponse(saved);
	}

	@Override
	@Transactional
	public BookingResponse cancel(UUID userId, Long bookingId) {
		Booking entity = bookingRepository.findById(bookingId)
			.orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

		// Verify the booking belongs to the user
		if (!entity.getClient().getId().equals(userId)) {
			throw new IllegalArgumentException("Booking does not belong to user");
		}

		// Validate that booking start time is not in the past
		Instant now = Instant.now();
		if (entity.getStartTime().isBefore(now) || entity.getStartTime().equals(now)) {
			throw new IllegalArgumentException("Cannot cancel a booking that has already started or passed");
		}

		// Validate cancellation rules
		BookingStatus currentStatus = entity.getStatus();
		if (currentStatus == BookingStatus.PENDING_APPROVAL) {
			// Allow cancellation for pending bookings
			entity.setStatus(BookingStatus.CANCELLED);
		} else if (currentStatus == BookingStatus.CONFIRMED) {
			// Not allowed to cancel confirmed bookings
			throw new IllegalArgumentException("Cannot cancel a booking with CONFIRMED status");
		} else {
			// For other statuses (DECLINED, CANCELLED, COMPLETED), throw error
			throw new IllegalArgumentException("Cannot cancel a booking with status: " + currentStatus);
		}

		Booking saved = bookingRepository.save(entity);
		return toResponse(saved);
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
	@Transactional(readOnly = true)
	public Page<Booking> getAllBookingsByStatus(BookingStatus status, Pageable pageable) {
		return bookingRepository.findByStatusOrderByStartTimeAsc(status, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public AdminBookingsGroupedByStatusResponse getBookingsGroupedByStatus(Set<BookingStatus> statuses) {
		if (statuses == null || statuses.isEmpty()) {
			return new AdminBookingsGroupedByStatusResponse();
		}

		List<Booking> bookings = bookingRepository.findByStatusInOrderByStartTimeAsc(statuses);
		
		// Group bookings by status
		Map<BookingStatus, List<Booking>> bookingsByStatus = bookings.stream()
			.collect(Collectors.groupingBy(Booking::getStatus));

		// Convert to response DTO
		AdminBookingsGroupedByStatusResponse response = new AdminBookingsGroupedByStatusResponse();
		for (BookingStatus status : statuses) {
			List<Booking> statusBookings = bookingsByStatus.getOrDefault(status, List.of());
			List<AdminBookingResponse> adminBookings = statusBookings.stream()
				.map(booking -> {
					BookingResponse bookingResponse = toResponse(booking);
					return new AdminBookingResponse(bookingResponse, booking.getClient());
				})
				.sorted(Comparator.comparing(AdminBookingResponse::getStartTimeInstant))
				.collect(Collectors.toList());
			response.addBookingsForStatus(status, adminBookings);
		}

		return response;
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

		// Send email notifications for CONFIRMED or DECLINED only if email notifications are enabled
		User client = booking.getClient();
		boolean emailEnabled = userSettingsService.isEmailNotificationEnabled(client.getId());
		
		if (newStatus == BookingStatus.CONFIRMED && emailEnabled) {
			try {
				emailService.sendBookingConfirmationEmail(
					client.getEmail(),
					client.getFirstName(),
					client.getLastName(),
					booking.getSessionName(),
					booking.getStartTime()
				);
			} catch (Exception e) {
				System.err.println("Failed to send booking confirmation email: " + e.getMessage());
			}
		} else if (newStatus == BookingStatus.DECLINED && emailEnabled) {
			try {
				emailService.sendBookingRejectionEmail(
					client.getEmail(),
					client.getFirstName(),
					client.getLastName(),
					booking.getSessionName(),
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

	private static Map<String, BigDecimal> filterPricesByCurrency(Map<String, BigDecimal> prices, Currency currency) {
		Map<String, BigDecimal> filtered = new java.util.HashMap<>();
		if (prices != null) {
			String currencyKey = currency.getDisplayName();
			BigDecimal price = prices.get(currencyKey);
			filtered.put(currencyKey, price);
		}
		
		return filtered;
	}

	private static BookingResponse toResponse(Booking entity) {
		BookingResponse resp = new BookingResponse();
		resp.setId(entity.getId());
		resp.setSessionName(entity.getSessionName());
		resp.setSessionDurationMinutes(entity.getSessionDurationMinutes());
		resp.setSessionBufferMinutes(entity.getSessionBufferMinutes());
		resp.setSessionPrices(entity.getSessionPrices());
		resp.setSessionDescription(entity.getSessionDescription());
		resp.setStartTimeInstant(entity.getStartTime());
		resp.setEndTimeInstant(entity.getEndTime());
		resp.setStatus(entity.getStatus());
		resp.setClientMessage(entity.getClientMessage());
		resp.setCreatedAt(entity.getCreatedAt());
		return resp;
	}

	private static AdminBookingResponse toAdminResponse(Booking entity) {
		BookingResponse bookingResponse = toResponse(entity);
		return new AdminBookingResponse(bookingResponse, entity.getClient());
	}
}
