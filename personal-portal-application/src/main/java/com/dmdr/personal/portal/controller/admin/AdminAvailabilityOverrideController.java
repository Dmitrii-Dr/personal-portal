package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.booking.dto.availability.override.AvailabilityOverrideResponse;
import com.dmdr.personal.portal.booking.dto.availability.override.CreateAvailabilityOverrideRequest;
import com.dmdr.personal.portal.booking.dto.availability.override.UpdateAvailabilityOverrideRequest;
import com.dmdr.personal.portal.booking.service.AvailabilityOverrideService;
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
@RequestMapping("/api/v1/admin/booking/availability/override")
public class AdminAvailabilityOverrideController {

	private final AvailabilityOverrideService availabilityOverrideService;

	public AdminAvailabilityOverrideController(AvailabilityOverrideService availabilityOverrideService) {
		this.availabilityOverrideService = availabilityOverrideService;
	}

	@GetMapping
	public ResponseEntity<List<AvailabilityOverrideResponse>> getAll() {
		return ResponseEntity.ok(availabilityOverrideService.getAll());
	}

	@PostMapping
	public ResponseEntity<AvailabilityOverrideResponse> create(@Valid @RequestBody CreateAvailabilityOverrideRequest request) {
		AvailabilityOverrideResponse response = availabilityOverrideService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/{id}")
	public ResponseEntity<AvailabilityOverrideResponse> update(
		@PathVariable Long id,
		@Valid @RequestBody UpdateAvailabilityOverrideRequest request
	) {
		return ResponseEntity.ok(availabilityOverrideService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		availabilityOverrideService.delete(id);
		return ResponseEntity.noContent().build();
	}
}

