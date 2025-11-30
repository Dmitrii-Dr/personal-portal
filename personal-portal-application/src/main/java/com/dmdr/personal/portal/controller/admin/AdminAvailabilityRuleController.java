package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.booking.dto.availability.rule.AvailabilityRuleResponse;
import com.dmdr.personal.portal.booking.dto.availability.rule.CreateAvailabilityRuleRequest;
import com.dmdr.personal.portal.booking.dto.availability.rule.UpdateAvailabilityRuleRequest;
import com.dmdr.personal.portal.booking.service.AvailabilityRuleService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/booking/availability/rule")
public class AdminAvailabilityRuleController {

	private final AvailabilityRuleService availabilityRuleService;

	public AdminAvailabilityRuleController(AvailabilityRuleService availabilityRuleService) {
		this.availabilityRuleService = availabilityRuleService;
	}

	@GetMapping
	public ResponseEntity<List<AvailabilityRuleResponse>> list() {
		return ResponseEntity.ok(availabilityRuleService.getAll());
	}

	@GetMapping("/active")
	public ResponseEntity<List<AvailabilityRuleResponse>> listActive() {
		return ResponseEntity.ok(availabilityRuleService.getAllActive());
	}

	@PostMapping
	public ResponseEntity<AvailabilityRuleResponse> create(
		@Valid @RequestBody CreateAvailabilityRuleRequest request
	) {
		return ResponseEntity.ok(availabilityRuleService.create(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<AvailabilityRuleResponse> update(
		@PathVariable("id") Long id,
		@Valid @RequestBody UpdateAvailabilityRuleRequest request
	) {
		if (request.getId() == null || !id.equals(request.getId())) {
			throw new IllegalArgumentException("Path variable id (" + id + ") must match request body id (" + request.getId() + ")");
		}
		return ResponseEntity.ok(availabilityRuleService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
		availabilityRuleService.delete(id);
		return ResponseEntity.noContent().build();
	}
}


