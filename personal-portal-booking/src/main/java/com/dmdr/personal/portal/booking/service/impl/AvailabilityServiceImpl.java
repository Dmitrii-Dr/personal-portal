package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.booking.BookingSuggestion;
import com.dmdr.personal.portal.booking.model.AvailabilityRule;
import com.dmdr.personal.portal.booking.model.BookingSettings;
import com.dmdr.personal.portal.booking.repository.AvailabilityRuleRepository;
import com.dmdr.personal.portal.booking.repository.BookingSettingsRepository;
import com.dmdr.personal.portal.booking.service.AvailabilityService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityServiceImpl implements AvailabilityService {

	private final AvailabilityRuleRepository repository;
	private final BookingSettingsRepository bookingSettingsRepository;

	public AvailabilityServiceImpl(
		AvailabilityRuleRepository repository,
		BookingSettingsRepository bookingSettingsRepository
	) {
		this.repository = repository;
		this.bookingSettingsRepository = bookingSettingsRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public void validateBookingAvailability(Instant requestedStartTime, Instant requestedEndTime) {
		List<AvailabilityRule> activeRules = repository.findByRuleStatus(AvailabilityRule.RuleStatus.ACTIVE);

		// Filter rules that match the booking request
		List<AvailabilityRule> matchingRules = activeRules.stream()
			.filter(rule -> {
				// Check if rule is valid during the booking time period (both start and end times)
				if (!isRuleTimeFit(rule, requestedStartTime, requestedEndTime)) {
					return false;
				}

				ZoneId zoneId = ZoneId.of(rule.getTimezone());

				// Convert booking startTime to LocalTime and DayOfWeek using rule's timezone
				LocalTime bookingStartTime = requestedStartTime.atZone(zoneId).toLocalTime();
				LocalTime bookingEndTime = requestedEndTime.atZone(zoneId).toLocalTime();
				DayOfWeek bookingDayOfWeek = requestedStartTime.atZone(zoneId).getDayOfWeek();

				// Check if booking day is in rule's daysOfWeekAsInt
				int dayValue = bookingDayOfWeek.getValue();
				if (!containsDay(rule.getDaysOfWeekAsInt(), dayValue)) {
					return false;
				}

				// Check if booking time is within rule's available hours
				// Both start and end times must be within available hours
				return isTimeWithinAvailableHours(rule, bookingStartTime, bookingEndTime);
			})
			.collect(Collectors.toList());

		// Validate that exactly one rule matches
		if (matchingRules.isEmpty()) {
			throw new IllegalArgumentException("No availability rule found for the requested booking date and time");
		}

		if (matchingRules.size() > 1) {
			throw new IllegalStateException(
				"Multiple availability rules (" + matchingRules.size() + ") found for the requested booking date and time. This should not happen.");
		}
	}

	private boolean isRuleTimeFit(AvailabilityRule rule, Instant bookingStartTime, Instant bookingEndTime) {
		Instant ruleStart = rule.getRuleStartInstant();
		Instant ruleEnd = rule.getRuleEndInstant();

		// Check if booking start time is within rule's valid period [ruleStart, ruleEnd]
		boolean startAfterOrEqual = bookingStartTime.isAfter(ruleStart) || bookingStartTime.equals(ruleStart);
		boolean startBeforeOrEqual = bookingStartTime.isBefore(ruleEnd) || bookingStartTime.equals(ruleEnd);
		boolean startWithinRule = startAfterOrEqual && startBeforeOrEqual;

		// Check if booking end time is within rule's valid period [ruleStart, ruleEnd]
		boolean endAfterOrEqual = bookingEndTime.isAfter(ruleStart) || bookingEndTime.equals(ruleStart);
		boolean endBeforeOrEqual = bookingEndTime.isBefore(ruleEnd) || bookingEndTime.equals(ruleEnd);
		boolean endWithinRule = endAfterOrEqual && endBeforeOrEqual;

		// Both start and end times must be within the rule's time period
		return startWithinRule && endWithinRule;
	}

	private boolean isTimeWithinAvailableHours(AvailabilityRule rule, LocalTime bookingStartTime, LocalTime bookingEndTime) {
		LocalTime availableStart = rule.getAvailableStartTime();
		LocalTime availableEnd = rule.getAvailableEndTime();

		// Check if booking start time is within available hours [availableStart, availableEnd]
		boolean startAfterOrEqual = bookingStartTime.isAfter(availableStart) || bookingStartTime.equals(availableStart);
		boolean startBeforeOrEqual = bookingStartTime.isBefore(availableEnd) || bookingStartTime.equals(availableEnd);
		boolean startWithinHours = startAfterOrEqual && startBeforeOrEqual;

		// Check if booking end time is within available hours [availableStart, availableEnd]
		boolean endAfterOrEqual = bookingEndTime.isAfter(availableStart) || bookingEndTime.equals(availableStart);
		boolean endBeforeOrEqual = bookingEndTime.isBefore(availableEnd) || bookingEndTime.equals(availableEnd);
		boolean endWithinHours = endAfterOrEqual && endBeforeOrEqual;

		return startWithinHours && endWithinHours;
	}

	@Override
	@Transactional(readOnly = true)
	public List<BookingSuggestion> calculateBookingSuggestion(
		int sessionDurationMinutes,
		LocalDate suggestedDate,
		String timezone
	) {
		ZoneId zoneId = ZoneId.of(timezone);

		// Convert suggestedDate to start and end of day in the given timezone
		LocalDateTime startOfDay = suggestedDate.atStartOfDay();
		LocalDateTime endOfDay = suggestedDate.atTime(23, 59, 59, 999_999_999);
		
        Instant dayStartInstant = startOfDay.atZone(zoneId).toInstant();
		Instant dayEndInstant = endOfDay.atZone(zoneId).toInstant();

		// Load only ACTIVE availability rules
		List<AvailabilityRule> activeRules = repository.findByRuleStatus(AvailabilityRule.RuleStatus.ACTIVE);

		// Find rules that match the suggested date
		List<AvailabilityRule> matchingRules = activeRules.stream()
			.filter(rule -> {
				// Check if rule overlaps with the day
				// Two intervals overlap if: ruleStart <= dayEnd AND ruleEnd >= dayStart
				Instant ruleStart = rule.getRuleStartInstant();
				Instant ruleEnd = rule.getRuleEndInstant();

				boolean ruleOverlapsDay = (ruleStart.isBefore(dayEndInstant) || ruleStart.equals(dayEndInstant))
					&& (ruleEnd.isAfter(dayStartInstant) || ruleEnd.equals(dayStartInstant));

				if (!ruleOverlapsDay) {
					return false;
				}

				// Get day of week in the rule's timezone
				// Convert suggestedDate to the rule's timezone to get the correct day of week
				ZoneId ruleZoneId = ZoneId.of(rule.getTimezone());
				LocalDate dateInRuleTimezone = suggestedDate.atStartOfDay().atZone(zoneId)
					.withZoneSameInstant(ruleZoneId)
					.toLocalDate();
				DayOfWeek dayOfWeekInRuleTimezone = dateInRuleTimezone.getDayOfWeek();

				// Check if the day of week matches
				int dayValue = dayOfWeekInRuleTimezone.getValue();
				return containsDay(rule.getDaysOfWeekAsInt(), dayValue);
			})
			.collect(Collectors.toList());

		if (matchingRules.isEmpty()) {
			return new ArrayList<>();
		}

		// Get booking settings for slot interval
		BookingSettings settings = bookingSettingsRepository.findTopByOrderByIdAsc()
			.orElseGet(() -> {
				BookingSettings s = new BookingSettings();
				s.setBookingSlotsInterval(15);
				return s;
			});

		// Collect working hours from all matching rules and find overlap with client's day
		// Note: Different rules' working hours don't overlap, so we process each separately
		List<BookingSuggestion> suggestions = new ArrayList<>();
		for (AvailabilityRule rule : matchingRules) {
			ZoneId ruleZoneId = ZoneId.of(rule.getTimezone());
			LocalTime availableStart = rule.getAvailableStartTime();
			LocalTime availableEnd = rule.getAvailableEndTime();

			// Convert suggestedDate to the rule's timezone to get the correct date
			LocalDate dateInRuleTimezone = suggestedDate.atStartOfDay().atZone(zoneId)
				.withZoneSameInstant(ruleZoneId)
				.toLocalDate();

			// Get working hours for that date in the rule's timezone
			LocalDateTime ruleStartDateTime = dateInRuleTimezone.atTime(availableStart);
			LocalDateTime ruleEndDateTime = dateInRuleTimezone.atTime(availableEnd);
			Instant ruleWorkingStartInstant = ruleStartDateTime.atZone(ruleZoneId).toInstant();
			Instant ruleWorkingEndInstant = ruleEndDateTime.atZone(ruleZoneId).toInstant();

			// Find overlap between client's day and rule's working hours
			TimeRange overlap = findOverlap(
				new TimeRange(dayStartInstant, dayEndInstant),
				new TimeRange(ruleWorkingStartInstant, ruleWorkingEndInstant)
			);

			if (overlap != null) {
				// Generate booking suggestions for this rule's working hours
				suggestions.addAll(generateSlots(overlap, sessionDurationMinutes, settings.getBookingSlotsInterval()));
			}
		}

		return suggestions;
	}


	private List<BookingSuggestion> generateSlots(TimeRange range, int sessionDurationMinutes, int slotIntervalMinutes) {
		List<BookingSuggestion> slots = new ArrayList<>();
		Instant currentStart = range.getStart();

		while (true) {
			Instant slotEnd = currentStart.plusSeconds(sessionDurationMinutes * 60L);

			// Check if slot end exceeds the range end
			if (slotEnd.isAfter(range.getEnd())) {
				break;
			}

			BookingSuggestion suggestion = new BookingSuggestion();
			suggestion.setStartTime(currentStart);
			suggestion.setEndTime(slotEnd);
			slots.add(suggestion);

			// Move to next slot start (current start + interval)
			currentStart = currentStart.plusSeconds(slotIntervalMinutes * 60L);

			// Check if next slot start would exceed the range
			if (currentStart.isAfter(range.getEnd()) || currentStart.equals(range.getEnd())) {
				break;
			}
		}

		return slots;
	}

	private TimeRange findOverlap(TimeRange range1, TimeRange range2) {
		Instant overlapStart = range1.getStart().isAfter(range2.getStart()) ? range1.getStart() : range2.getStart();
		Instant overlapEnd = range1.getEnd().isBefore(range2.getEnd()) ? range1.getEnd() : range2.getEnd();

		// Check if there's actually an overlap (overlapStart must be before overlapEnd)
		boolean hasOverlap = overlapStart.isBefore(overlapEnd);
		if (!hasOverlap) {
			return null;
		}

		return new TimeRange(overlapStart, overlapEnd);
	}

	private static boolean containsDay(int[] daysOfWeekAsInt, int dayValue) {
		return Arrays.stream(daysOfWeekAsInt)
			.anyMatch(day -> day == dayValue);
	}

	private static class TimeRange {
		private final Instant start;
		private final Instant end;

		TimeRange(Instant start, Instant end) {
			this.start = start;
			this.end = end;
		}

		Instant getStart() {
			return start;
		}

		Instant getEnd() {
			return end;
		}
	}
}

