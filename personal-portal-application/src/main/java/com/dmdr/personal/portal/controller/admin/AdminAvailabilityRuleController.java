package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.booking.dto.availability.rule.AvailabilityRuleResponse;
import com.dmdr.personal.portal.booking.dto.availability.rule.CreateAvailabilityRuleRequest;
import com.dmdr.personal.portal.booking.dto.availability.rule.UpdateAvailabilityRuleRequest;
import com.dmdr.personal.portal.booking.service.AvailabilityRuleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class AdminAvailabilityRuleController {

	private final AvailabilityRuleService availabilityRuleService;

	public AdminAvailabilityRuleController(AvailabilityRuleService availabilityRuleService) {
		this.availabilityRuleService = availabilityRuleService;
	}

	@GetMapping
	public ResponseEntity<List<AvailabilityRuleResponse>> list(HttpServletRequest httpRequest) {
		String ctx = AdminApiLogSupport.http(httpRequest);
		log.info("BEGIN listAvailabilityRules {}", ctx);
		try {
			return ResponseEntity.ok(availabilityRuleService.getAll());
		} finally {
			log.info("END listAvailabilityRules {}", ctx);
		}
	}

	@GetMapping("/active")
	public ResponseEntity<List<AvailabilityRuleResponse>> listActive(HttpServletRequest httpRequest) {
		String ctx = AdminApiLogSupport.http(httpRequest);
		log.info("BEGIN listActiveAvailabilityRules {}", ctx);
		try {
			return ResponseEntity.ok(availabilityRuleService.getAllActive());
		} finally {
			log.info("END listActiveAvailabilityRules {}", ctx);
		}
	}

	@PostMapping
	public ResponseEntity<AvailabilityRuleResponse> create(
		@Valid @RequestBody CreateAvailabilityRuleRequest request
	) {
		int daysCount = request.getDaysOfWeek() != null ? request.getDaysOfWeek().size() : 0;
		String ctx = "ruleStatus=" + request.getRuleStatus()
			+ " daysCount=" + daysCount
			+ " ruleStartDate=" + request.getRuleStartDate();
		log.info("BEGIN createAvailabilityRule {}", ctx);
		try {
			return ResponseEntity.ok(availabilityRuleService.create(request));
		} finally {
			log.info("END createAvailabilityRule {}", ctx);
		}
	}

	@PutMapping("/{id}")
	public ResponseEntity<AvailabilityRuleResponse> update(
		@PathVariable("id") Long id,
		@Valid @RequestBody UpdateAvailabilityRuleRequest request
	) {
		String ctx = "ruleId=" + id + " ruleStatus=" + request.getRuleStatus();
		log.info("BEGIN updateAvailabilityRule {}", ctx);
		try {
			if (request.getId() == null || !id.equals(request.getId())) {
				throw new IllegalArgumentException("Path variable id (" + id + ") must match request body id (" + request.getId() + ")");
			}
			return ResponseEntity.ok(availabilityRuleService.update(id, request));
		} finally {
			log.info("END updateAvailabilityRule {}", ctx);
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
		String ctx = "ruleId=" + id;
		log.info("BEGIN deleteAvailabilityRule {}", ctx);
		try {
			availabilityRuleService.delete(id);
			return ResponseEntity.noContent().build();
		} finally {
			log.info("END deleteAvailabilityRule {}", ctx);
		}
	}
}


