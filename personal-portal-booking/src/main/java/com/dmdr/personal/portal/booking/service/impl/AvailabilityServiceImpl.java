package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.booking.BookingSuggestion;
import com.dmdr.personal.portal.booking.model.AvailabilityOverride;
import com.dmdr.personal.portal.booking.model.AvailabilityRule;
import com.dmdr.personal.portal.booking.model.BookingSettings;
import com.dmdr.personal.portal.booking.model.OverrideStatus;
import com.dmdr.personal.portal.booking.repository.AvailabilityOverrideRepository;
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
	private final AvailabilityOverrideRepository overrideRepository;

	public AvailabilityServiceImpl(
		AvailabilityRuleRepository repository,
		BookingSettingsRepository bookingSettingsRepository,
		AvailabilityOverrideRepository overrideRepository
	) {
		this.repository = repository;
		this.bookingSettingsRepository = bookingSettingsRepository;
		this.overrideRepository = overrideRepository;
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

		// Validate that dayStartInstant is not before the start of the current day
		LocalDate today = LocalDate.now(zoneId);
		Instant todayStartInstant = today.atStartOfDay().atZone(zoneId).toInstant();
		if (dayStartInstant.isBefore(todayStartInstant)) {
			throw new IllegalArgumentException(
				"Cannot calculate booking suggestions for a date in the past. " +
				"Suggested date: " + suggestedDate + " (" + timezone + "), " +
				"Day start time: " + dayStartInstant.atZone(zoneId) + ", " +
				"Today start time: " + todayStartInstant.atZone(zoneId)
			);
		}

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

		// Get booking settings for slot interval
		BookingSettings settings = bookingSettingsRepository.mustFindTopByOrderByIdAsc();

		// Calculate minimum start time (now + bookingFirstSlotInterval)
		// This ensures sessions cannot be booked too soon (e.g., not within 5 minutes, or not within 2 days)
		Instant minimumStartTime = Instant.now().plusSeconds(settings.getBookingFirstSlotInterval() * 60L);

		// Get unavailable overrides that reduce availability
		List<AvailabilityOverride> unavailableOverrides = overrideRepository.findOverlappingUnavailableOverrides(
			dayStartInstant, dayEndInstant, OverrideStatus.ACTIVE);

		// Collect working hours from all matching rules and find overlap with client's day
		// Note: Different rules' working hours don't overlap, so we process each separately
		List<TimeRange> ruleRanges = new ArrayList<>();
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

			// Find overlap between client's day and rule's working hours (filter past intervals)
			TimeRange ruleOverlap = findFutureOverlap(
				new TimeRange(dayStartInstant, dayEndInstant),
				new TimeRange(ruleWorkingStartInstant, ruleWorkingEndInstant),
				minimumStartTime
			);

			if (ruleOverlap != null) {
				ruleRanges.add(ruleOverlap);
			}
		}

		// Subtract unavailable override ranges from rule ranges
		List<TimeRange> allAvailableRanges = subtractUnavailableRangesFromAll(ruleRanges, unavailableOverrides);

		// Always check for availability overrides with isAvailable = true
		// These overrides extend availability beyond normal rules
		List<AvailabilityOverride> availableOverrides = overrideRepository.findOverlappingAvailableOverrides(
			dayStartInstant, dayEndInstant, OverrideStatus.ACTIVE);

		for (AvailabilityOverride override : availableOverrides) {
			// Calculate overlap between override and client's day (filter past intervals if today)
			TimeRange overrideOverlap = findFutureOverlap(
				new TimeRange(dayStartInstant, dayEndInstant),
				new TimeRange(override.getOverideStartInstant(), override.getOverrideEndInstant()),
				minimumStartTime
			);

			if (overrideOverlap != null) {
				allAvailableRanges.add(overrideOverlap);
			}
		}

		// Generate booking suggestions from all remaining available time ranges
		List<BookingSuggestion> suggestions = new ArrayList<>();
		for (TimeRange availableRange : allAvailableRanges) {
			suggestions.addAll(generateSlots(availableRange, sessionDurationMinutes, settings.getBookingSlotsInterval()));
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

	private TimeRange findFutureOverlap(TimeRange range1, TimeRange range2, Instant minimumStartTime) {
		Instant overlapStart = range1.getStart().isAfter(range2.getStart()) ? range1.getStart() : range2.getStart();
		Instant overlapEnd = range1.getEnd().isBefore(range2.getEnd()) ? range1.getEnd() : range2.getEnd();

		// Check if there's actually an overlap (overlapStart must be before overlapEnd)
		boolean hasOverlap = overlapStart.isBefore(overlapEnd);
		if (!hasOverlap) {
			return null;
		}

		// If minimumStartTime is provided (for today's bookings), ensure overlap starts after it
		if (minimumStartTime != null) {
			if (overlapStart.isBefore(minimumStartTime)) {
				// Adjust overlap start to minimumStartTime if it starts earlier
				overlapStart = minimumStartTime;
				// Check if there's still valid overlap after adjustment
				if (!overlapStart.isBefore(overlapEnd)) {
					return null;
				}
			}
		} else {
			// For future dates, ensure overlap doesn't start in the past
			Instant now = Instant.now();
			if (overlapStart.isBefore(now) || overlapStart.equals(now)) {
				return null;
			}
		}

		return new TimeRange(overlapStart, overlapEnd);
	}

	/**
	 * Subtracts unavailable override ranges from all available time ranges.
	 * Returns a list of remaining available time ranges after excluding unavailable periods.
	 */
	private List<TimeRange> subtractUnavailableRangesFromAll(
		List<TimeRange> availableRanges,
		List<AvailabilityOverride> unavailableOverrides
	) {
		if (unavailableOverrides.isEmpty()) {
			return availableRanges;
		}

		List<TimeRange> unavailableRanges = unavailableOverrides.stream()
			.map(override -> new TimeRange(
				override.getOverideStartInstant(),
				override.getOverrideEndInstant()
			))
			.collect(Collectors.toList());

		List<TimeRange> result = new ArrayList<>(availableRanges);

		// Apply each unavailable range to all current available ranges
		for (TimeRange unavailableRange : unavailableRanges) {
			List<TimeRange> newResult = new ArrayList<>();
			for (TimeRange currentRange : result) {
				// Subtract unavailable range from current range
				List<TimeRange> remaining = subtractRange(currentRange, unavailableRange);
				newResult.addAll(remaining);
			}
			result = newResult;
		}

		return result;
	}

	/**
	 * Subtracts a range from another range, returning the remaining parts.
	 * Can return 0, 1, or 2 ranges depending on how the ranges overlap.
	 */
	private List<TimeRange> subtractRange(TimeRange original, TimeRange toSubtract) {
		List<TimeRange> result = new ArrayList<>();

		// Check if ranges overlap (no minimum start time restriction for subtraction)
		TimeRange overlap = findOverlap(original, toSubtract);
		if (overlap == null) {
			// No overlap, return original range
			result.add(original);
			return result;
		}

		// If overlap completely covers original (overlap equals original), return empty list
		if (overlap.getStart().equals(original.getStart()) && overlap.getEnd().equals(original.getEnd())) {
			return result;
		}

		// Check if there's a part before the overlap
		if (original.getStart().isBefore(overlap.getStart())) {
			result.add(new TimeRange(original.getStart(), overlap.getStart()));
		}

		// Check if there's a part after the overlap
		if (original.getEnd().isAfter(overlap.getEnd())) {
			result.add(new TimeRange(overlap.getEnd(), original.getEnd()));
		}

		return result;
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

