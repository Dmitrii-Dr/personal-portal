package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.booking.dto.booking.AdminBookingSettingsResponse;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingSettingsRequest;
import com.dmdr.personal.portal.booking.service.BookingSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/booking/setting")
@Slf4j
public class AdminBookingSettingController {

	private final BookingSettingsService bookingSettingsService;

	public AdminBookingSettingController(BookingSettingsService bookingSettingsService) {
		this.bookingSettingsService = bookingSettingsService;
	}

	@GetMapping
	public ResponseEntity<AdminBookingSettingsResponse> getSettings(HttpServletRequest httpRequest) {
		String ctx = AdminApiLogSupport.http(httpRequest);
		log.info("BEGIN getBookingSettings {}", ctx);
		try {
			return ResponseEntity.ok(bookingSettingsService.getSettings());
		} finally {
			log.info("END getBookingSettings {}", ctx);
		}
	}

	@PutMapping
	public ResponseEntity<AdminBookingSettingsResponse> updateSettings(
		HttpServletRequest httpRequest,
		@Valid @RequestBody UpdateBookingSettingsRequest request
	) {
		String ctx = AdminApiLogSupport.http(httpRequest)
			+ " defaultTimezoneId=" + request.getDefaultTimezoneId()
			+ " roundBookingSuggestions=" + request.isRoundBookingSuggestions();
		log.info("BEGIN updateBookingSettings {}", ctx);
		try {
			return ResponseEntity.ok(bookingSettingsService.updateSettings(request));
		} finally {
			log.info("END updateBookingSettings {}", ctx);
		}
	}
}


