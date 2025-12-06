package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.booking.dto.booking.BookingResponse;
import com.dmdr.personal.portal.booking.dto.booking.BookingsGroupedByStatusResponse;
import com.dmdr.personal.portal.booking.dto.booking.CreateBookingRequest;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingRequest;
import com.dmdr.personal.portal.booking.model.BookingStatus;
import com.dmdr.personal.portal.booking.service.BookingService;
import com.dmdr.personal.portal.controller.util.BookingStatusParser;
import com.dmdr.personal.portal.service.CurrentUserService;
import com.dmdr.personal.portal.users.model.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/session/booking")
public class BookingController {

	private final BookingService bookingService;
	private final CurrentUserService currentUserService;

	public BookingController(BookingService bookingService, CurrentUserService currentUserService) {
		this.bookingService = bookingService;
		this.currentUserService = currentUserService;
	}

	@GetMapping
	public ResponseEntity<List<BookingResponse>> getBookingsByStatuses(
			@RequestParam(value = "status", required = false) String statusParam) {
		User currentUser = currentUserService.getCurrentUser();
		Set<BookingStatus> statuses = BookingStatusParser.parseStatuses(statusParam);
		List<BookingResponse> response = bookingService.getBookingsByStatusesForUser(currentUser.getId(), statuses);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/group")
	public ResponseEntity<BookingsGroupedByStatusResponse> getBookingsGroupedByStatuses(
			@RequestParam(value = "status", required = false) String statusParam) {
		User currentUser = currentUserService.getCurrentUser();
		Set<BookingStatus> statuses = BookingStatusParser.parseStatuses(statusParam);
		BookingsGroupedByStatusResponse response = bookingService.getBookingsGroupedByStatusForUser(currentUser.getId(), statuses);
		return ResponseEntity.ok(response);
	}

	@PostMapping
	public ResponseEntity<BookingResponse> create(@Valid @RequestBody CreateBookingRequest request) {
		User currentUser = currentUserService.getCurrentUser();
		BookingResponse response = bookingService.create(currentUser.getId(), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/{id}")
	public ResponseEntity<BookingResponse> update(
		@PathVariable Long id,
		@Valid @RequestBody UpdateBookingRequest request
	) {
		// Validate that path variable id matches request body id
		if (!id.equals(request.getId())) {
			throw new IllegalArgumentException("Path variable id does not match request body id");
		}

		// Validate that startTime is in the future
		Instant now = Instant.now();
		if (request.getStartTime().isBefore(now) || request.getStartTime().equals(now)) {
			throw new IllegalArgumentException("Start time must be in the future");
		}
		
		User currentUser = currentUserService.getCurrentUser();
		return ResponseEntity.ok(bookingService.update(currentUser.getId(), request));
	}

	@PostMapping("/{id}/cancel")
	public ResponseEntity<BookingResponse> cancel(@PathVariable Long id) {
		User currentUser = currentUserService.getCurrentUser();
		BookingResponse response = bookingService.cancel(currentUser.getId(), id);
		return ResponseEntity.ok(response);
	}
}

