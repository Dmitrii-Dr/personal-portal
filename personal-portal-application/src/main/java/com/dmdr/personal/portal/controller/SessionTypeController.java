package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.booking.dto.SessionTypeResponse;
import com.dmdr.personal.portal.booking.service.SessionTypeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/session/type")
public class SessionTypeController {

	private final SessionTypeService sessionTypeService;

	public SessionTypeController(SessionTypeService sessionTypeService) {
		this.sessionTypeService = sessionTypeService;
	}

	@GetMapping
	public ResponseEntity<List<SessionTypeResponse>> getAll() {
		return ResponseEntity.ok(sessionTypeService.getAll());
	}
}

