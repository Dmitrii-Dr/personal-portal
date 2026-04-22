package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.booking.dto.SessionTypeResponse;
import com.dmdr.personal.portal.booking.dto.CreateSessionTypeRequest;
import com.dmdr.personal.portal.booking.dto.UpdateSessionTypeRequest;
import com.dmdr.personal.portal.booking.service.SessionTypeService;
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

@RestController
@RequestMapping("/api/v1/admin/session/type")
@Slf4j
public class AdminSessionTypeController {

	private final SessionTypeService sessionTypeService;

	public AdminSessionTypeController(SessionTypeService sessionTypeService) {
		this.sessionTypeService = sessionTypeService;
	}

	@PostMapping
	public ResponseEntity<SessionTypeResponse> create(@Valid @RequestBody CreateSessionTypeRequest request) {
		int nameLen = request.getName() != null ? request.getName().length() : 0;
		String ctx = "durationMinutes=" + request.getDurationMinutes()
			+ " bufferMinutes=" + request.getBufferMinutes()
			+ " nameLength=" + nameLen;
		log.info("BEGIN createSessionType {}", ctx);
		try {
			SessionTypeResponse response = sessionTypeService.create(request);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} finally {
			log.info("END createSessionType {}", ctx);
		}
	}

	@PutMapping("/{id}")
	public ResponseEntity<SessionTypeResponse> update(
		@PathVariable Long id,
		@Valid @RequestBody UpdateSessionTypeRequest request
	) {
		String ctx = "sessionTypeId=" + id
			+ " durationMinutes=" + request.getDurationMinutes()
			+ " active=" + request.getActive();
		log.info("BEGIN updateSessionType {}", ctx);
		try {
			return ResponseEntity.ok(sessionTypeService.update(id, request));
		} finally {
			log.info("END updateSessionType {}", ctx);
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		String ctx = "sessionTypeId=" + id;
		log.info("BEGIN deleteSessionType {}", ctx);
		try {
			sessionTypeService.delete(id);
			return ResponseEntity.noContent().build();
		} finally {
			log.info("END deleteSessionType {}", ctx);
		}
	}

	@GetMapping("/all")
	public ResponseEntity<java.util.List<SessionTypeResponse>> getAllIncludingInactive(HttpServletRequest httpRequest) {
		String ctx = AdminApiLogSupport.http(httpRequest);
		log.info("BEGIN getAllSessionTypesIncludingInactive {}", ctx);
		try {
			return ResponseEntity.ok(sessionTypeService.getAllIncludingInactive());
		} finally {
			log.info("END getAllSessionTypesIncludingInactive {}", ctx);
		}
	}
}

