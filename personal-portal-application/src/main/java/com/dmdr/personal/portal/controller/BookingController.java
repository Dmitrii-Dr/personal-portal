package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.booking.dto.booking.BookingResponse;
import com.dmdr.personal.portal.booking.dto.booking.CreateBookingRequest;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingRequest;
import com.dmdr.personal.portal.booking.service.BookingService;
import com.dmdr.personal.portal.service.CurrentUserService;
import com.dmdr.personal.portal.users.model.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
	public ResponseEntity<List<BookingResponse>> getAll() {
		User currentUser = currentUserService.getCurrentUser();
		return ResponseEntity.ok(bookingService.getAllForUser(currentUser.getId()));
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
		User currentUser = currentUserService.getCurrentUser();
		return ResponseEntity.ok(bookingService.update(currentUser.getId(), request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		User currentUser = currentUserService.getCurrentUser();
		bookingService.delete(currentUser.getId(), id);
		return ResponseEntity.noContent().build();
	}
}

