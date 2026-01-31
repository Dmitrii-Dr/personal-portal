package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.booking.dto.booking.BookingSettingsResponse;
import com.dmdr.personal.portal.booking.service.BookingSettingsPublicService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/booking/setting")
public class BookingSettingController {

	private final BookingSettingsPublicService bookingSettingsPublicService;

	public BookingSettingController(BookingSettingsPublicService bookingSettingsPublicService) {
		this.bookingSettingsPublicService = bookingSettingsPublicService;
	}

	@GetMapping
	public ResponseEntity<BookingSettingsResponse> getIntervals() {
		return ResponseEntity.ok(bookingSettingsPublicService.getIntervals());
	}
}
