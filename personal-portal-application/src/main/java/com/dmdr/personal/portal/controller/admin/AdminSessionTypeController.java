package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.booking.dto.SessionTypeResponse;
import com.dmdr.personal.portal.booking.dto.CreateSessionTypeRequest;
import com.dmdr.personal.portal.booking.dto.UpdateSessionTypeRequest;
import com.dmdr.personal.portal.booking.service.SessionTypeService;
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

@RestController
@RequestMapping("/api/v1/admin/session/type")
public class AdminSessionTypeController {

	private final SessionTypeService sessionTypeService;

	public AdminSessionTypeController(SessionTypeService sessionTypeService) {
		this.sessionTypeService = sessionTypeService;
	}

	@PostMapping
	public ResponseEntity<SessionTypeResponse> create(@Valid @RequestBody CreateSessionTypeRequest request) {
		SessionTypeResponse response = sessionTypeService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/{id}")
	public ResponseEntity<SessionTypeResponse> update(
		@PathVariable Long id,
		@Valid @RequestBody UpdateSessionTypeRequest request
	) {
		return ResponseEntity.ok(sessionTypeService.update(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		sessionTypeService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/all")
	public ResponseEntity<java.util.List<SessionTypeResponse>> getAllIncludingInactive() {
		return ResponseEntity.ok(sessionTypeService.getAllIncludingInactive());
	}
}

