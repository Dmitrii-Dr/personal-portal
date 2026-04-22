package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.booking.dto.availability.override.AvailabilityOverrideResponse;
import com.dmdr.personal.portal.booking.dto.availability.override.CreateAvailabilityOverrideRequest;
import com.dmdr.personal.portal.booking.dto.availability.override.UpdateAvailabilityOverrideRequest;
import com.dmdr.personal.portal.booking.service.AvailabilityOverrideService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AdminAvailabilityOverrideController {

	private final AvailabilityOverrideService availabilityOverrideService;

	public AdminAvailabilityOverrideController(AvailabilityOverrideService availabilityOverrideService) {
		this.availabilityOverrideService = availabilityOverrideService;
	}

	@GetMapping
	public ResponseEntity<List<AvailabilityOverrideResponse>> getAll(HttpServletRequest httpRequest) {
		String ctx = AdminApiLogSupport.http(httpRequest);
		log.info("BEGIN getAllAvailabilityOverrides {}", ctx);
		try {
			return ResponseEntity.ok(availabilityOverrideService.getAll());
		} finally {
			log.info("END getAllAvailabilityOverrides {}", ctx);
		}
	}

	@PostMapping
	public ResponseEntity<AvailabilityOverrideResponse> create(@Valid @RequestBody CreateAvailabilityOverrideRequest request) {
		String ctx = "overrideDate=" + request.getOverrideDate() + " isAvailable=" + request.isAvailable();
		log.info("BEGIN createAvailabilityOverride {}", ctx);
		try {
			AvailabilityOverrideResponse response = availabilityOverrideService.create(request);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} finally {
			log.info("END createAvailabilityOverride {}", ctx);
		}
	}

	@PutMapping("/{id}")
	public ResponseEntity<AvailabilityOverrideResponse> update(
		@PathVariable Long id,
		@Valid @RequestBody UpdateAvailabilityOverrideRequest request
	) {
		String ctx = "overrideId=" + id + " overrideDate=" + request.getOverrideDate();
		log.info("BEGIN updateAvailabilityOverride {}", ctx);
		try {
			return ResponseEntity.ok(availabilityOverrideService.update(id, request));
		} finally {
			log.info("END updateAvailabilityOverride {}", ctx);
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		String ctx = "overrideId=" + id;
		log.info("BEGIN deleteAvailabilityOverride {}", ctx);
		try {
			availabilityOverrideService.delete(id);
			return ResponseEntity.noContent().build();
		} finally {
			log.info("END deleteAvailabilityOverride {}", ctx);
		}
	}
}

