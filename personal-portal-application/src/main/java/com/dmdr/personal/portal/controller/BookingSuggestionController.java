package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.core.model.TimezoneEntry;
import com.dmdr.personal.portal.booking.dto.booking.BookingSuggestion;
import com.dmdr.personal.portal.booking.dto.booking.BookingSuggestionsResponse;
import com.dmdr.personal.portal.booking.model.SessionType;
import com.dmdr.personal.portal.booking.repository.SessionTypeRepository;
import com.dmdr.personal.portal.booking.service.AvailabilityService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/booking/available/slot")
public class BookingSuggestionController {

	private final AvailabilityService availabilityService;
	private final SessionTypeRepository sessionTypeRepository;

	public BookingSuggestionController(
			AvailabilityService availabilityService,
			SessionTypeRepository sessionTypeRepository) {
		this.availabilityService = availabilityService;
		this.sessionTypeRepository = sessionTypeRepository;
	}

	@GetMapping
	public ResponseEntity<BookingSuggestionsResponse> getBookingSuggestions(
			@RequestParam Long sessionTypeId,
			@RequestParam LocalDate suggestedDate,
			@RequestParam Integer timezoneId) {
		// Get session type to retrieve duration
		SessionType sessionType = sessionTypeRepository.findById(sessionTypeId)
				.orElseThrow(() -> new IllegalArgumentException("SessionType not found: " + sessionTypeId));

		// Calculate booking suggestions
		List<BookingSuggestion> suggestions = availabilityService.calculateBookingSuggestion(
				sessionType,
				suggestedDate,
				timezoneId);

		// Transform to DTO
		BookingSuggestionsResponse dto = transformToDto(suggestions, sessionTypeId, suggestedDate,
				TimezoneEntry.getById(timezoneId));

		return ResponseEntity.ok(dto);
	}

	private BookingSuggestionsResponse transformToDto(
			List<BookingSuggestion> suggestions,
			Long sessionTypeId,
			LocalDate date,
			TimezoneEntry timezoneEntry) {
		BookingSuggestionsResponse dto = new BookingSuggestionsResponse();
		dto.setDate(date);
		dto.setTimezone(timezoneEntry);
		dto.setSessionTypeId(sessionTypeId);

		// Calculate offset from timezone
		ZoneId zoneId = ZoneId.of(timezoneEntry.getGmtOffset());
		Instant now = Instant.now();
		ZoneOffset offset = zoneId.getRules().getOffset(now);
		dto.setOffset(offset.toString());

		// Transform suggestions to slots
		List<BookingSuggestionsResponse.Slot> slots = suggestions.stream()
				.sorted(Comparator.comparing(BookingSuggestion::getStartTimeInstant))
				.map(suggestion -> {
					BookingSuggestionsResponse.Slot slot = new BookingSuggestionsResponse.Slot();
					LocalDate localDateOfSuggestion = suggestion.getStartTimeInstant()
							.atZone(zoneId)
							.toLocalDate();
					LocalTime startTime = suggestion.getStartTime()
							.atZone(zoneId)
							.toLocalTime();
					LocalTime endTime = suggestion.getEndTime()
							.atZone(zoneId)
							.toLocalTime();
					slot.setDate(localDateOfSuggestion);
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
