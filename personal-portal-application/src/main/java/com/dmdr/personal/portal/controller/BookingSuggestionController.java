package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.booking.dto.booking.BookingSuggestion;
import com.dmdr.personal.portal.booking.dto.booking.BookingSuggestionsDto;
import com.dmdr.personal.portal.booking.model.SessionType;
import com.dmdr.personal.portal.booking.repository.SessionTypeRepository;
import com.dmdr.personal.portal.booking.service.AvailabilityService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/booking/available/slot")
public class BookingSuggestionController {

	private final AvailabilityService availabilityService;
	private final SessionTypeRepository sessionTypeRepository;

	public BookingSuggestionController(
		AvailabilityService availabilityService,
		SessionTypeRepository sessionTypeRepository
	) {
		this.availabilityService = availabilityService;
		this.sessionTypeRepository = sessionTypeRepository;
	}

	@GetMapping
	public ResponseEntity<BookingSuggestionsDto> getBookingSuggestions(
		@RequestParam Long sessionTypeId,
		@RequestParam LocalDate suggestedDate,
		@RequestParam String timezone
	) {
		// Get session type to retrieve duration
		SessionType sessionType = sessionTypeRepository.findById(sessionTypeId)
			.orElseThrow(() -> new IllegalArgumentException("SessionType not found: " + sessionTypeId));

		// Calculate booking suggestions
		List<BookingSuggestion> suggestions = availabilityService.calculateBookingSuggestion(
			sessionType.getDurationMinutes(),
			suggestedDate,
			timezone
		);

		// Transform to DTO
		BookingSuggestionsDto dto = transformToDto(suggestions, sessionTypeId, suggestedDate, timezone);

		return ResponseEntity.ok(dto);
	}

	private BookingSuggestionsDto transformToDto(
		List<BookingSuggestion> suggestions,
		Long sessionTypeId,
		LocalDate date,
		String timezone
	) {
		BookingSuggestionsDto dto = new BookingSuggestionsDto();
		dto.setDate(date);
		dto.setTimezone(timezone);
		dto.setSessionTypeId(sessionTypeId);

		// Calculate offset from timezone
		ZoneId zoneId = ZoneId.of(timezone);
		Instant now = Instant.now();
		ZoneOffset offset = zoneId.getRules().getOffset(now);
		dto.setOffset(offset.toString());

		// Transform suggestions to slots
		List<BookingSuggestionsDto.Slot> slots = suggestions.stream()
			.map(suggestion -> {
				BookingSuggestionsDto.Slot slot = new BookingSuggestionsDto.Slot();
				LocalTime startTime = suggestion.getStartTime()
					.atZone(zoneId)
					.toLocalTime();
				LocalTime endTime = suggestion.getEndTime()
					.atZone(zoneId)
					.toLocalTime();
				slot.setStartTime(startTime);
				slot.setEndTime(endTime);
				slot.setStartTimeInstant(suggestion.getStartTimeInstant());
				return slot;
			})
			.collect(Collectors.toList());

		dto.setSlots(slots);
		return dto;
	}
}
