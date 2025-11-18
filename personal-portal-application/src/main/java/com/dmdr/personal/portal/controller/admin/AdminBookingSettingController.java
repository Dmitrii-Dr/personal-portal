package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.booking.dto.booking.BookingSettingsResponse;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingSettingsRequest;
import com.dmdr.personal.portal.booking.service.BookingSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/booking/setting")
public class AdminBookingSettingController {

	private final BookingSettingsService bookingSettingsService;

	public AdminBookingSettingController(BookingSettingsService bookingSettingsService) {
		this.bookingSettingsService = bookingSettingsService;
	}

	@GetMapping
	public ResponseEntity<BookingSettingsResponse> getSettings() {
		return ResponseEntity.ok(bookingSettingsService.getSettings());
	}

	@PutMapping
	public ResponseEntity<BookingSettingsResponse> updateSettings(
		@Valid @RequestBody UpdateBookingSettingsRequest request
	) {
		return ResponseEntity.ok(bookingSettingsService.updateSettings(request));
	}
}


