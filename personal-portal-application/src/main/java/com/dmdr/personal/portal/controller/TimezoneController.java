package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.booking.dto.TimezoneResponse;
import com.dmdr.personal.portal.booking.service.TimezoneService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/timezone")
public class TimezoneController {

	private final TimezoneService timezoneService;

	public TimezoneController(TimezoneService timezoneService) {
		this.timezoneService = timezoneService;
	}

	@GetMapping
	public ResponseEntity<List<TimezoneResponse>> getAllTimezones() {
		return ResponseEntity.ok(timezoneService.getAllTimezones());
	}
}

