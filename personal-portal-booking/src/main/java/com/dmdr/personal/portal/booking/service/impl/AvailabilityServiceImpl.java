package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.booking.BookingSuggestion;
import com.dmdr.personal.portal.booking.model.AvailabilityOverride;
import com.dmdr.personal.portal.booking.model.AvailabilityRule;
import com.dmdr.personal.portal.booking.model.Booking;
import com.dmdr.personal.portal.booking.model.BookingSettings;
import com.dmdr.personal.portal.booking.model.BookingStatus;
import com.dmdr.personal.portal.booking.model.OverrideStatus;
import com.dmdr.personal.portal.booking.model.SessionType;
import com.dmdr.personal.portal.booking.repository.AvailabilityOverrideRepository;
import com.dmdr.personal.portal.booking.repository.AvailabilityRuleRepository;
import com.dmdr.personal.portal.booking.repository.BookingRepository;
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
	private final BookingRepository bookingRepository;

	public AvailabilityServiceImpl(
			AvailabilityRuleRepository repository,
			BookingSettingsRepository bookingSettingsRepository,
			AvailabilityOverrideRepository overrideRepository,
			BookingRepository bookingRepository) {
		this.repository = repository;
		this.bookingSettingsRepository = bookingSettingsRepository;
		this.overrideRepository = overrideRepository;
		this.bookingRepository = bookingRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public void validateBookingAvailability(Instant requestedStartTime, Instant requestedEndTime) {
		if (requestedStartTime.isAfter(requestedEndTime) || requestedStartTime.equals(requestedEndTime)) {
			throw new IllegalArgumentException("Requested start time must be before end time");
		}

		// Get booking settings first to get the default timezone
		BookingSettings settings = bookingSettingsRepository.mustFindTopByOrderByIdAsc();
		ZoneId zoneId = ZoneId.of(settings.getDefaultTimezone());
		LocalDate requestedDate = requestedStartTime.atZone(zoneId).toLocalDate();

		// Calculate day boundaries in the default timezone
		LocalDateTime startOfDay = requestedDate.atStartOfDay();
		LocalDateTime endOfDay = requestedDate.atTime(23, 59, 59, 999_999_999);
		Instant dayStartInstant = startOfDay.atZone(zoneId).toInstant();
		Instant dayEndInstant = endOfDay.atZone(zoneId).toInstant();

		// Follow the same logic as calculateBookingSuggestion:
		// 1. Find matching rules (may be empty, but availability can come from
		// overrides)
		// 2. Calculate available time ranges (which includes subtracting unavailable,
		// adding available overrides, and subtracting booked)
		List<TimeRange> allAvailableRanges = calculateAvailableTimeRanges(
				dayStartInstant, dayEndInstant, requestedDate, zoneId, settings);

		// Check if requested time range is fully contained in any available range
		TimeRange requestedRange = new TimeRange(requestedStartTime, requestedEndTime);
		boolean isAvailable = allAvailableRanges.stream()
				.anyMatch(availableRange -> {
					TimeRange overlap = findOverlap(availableRange, requestedRange);
					// The overlap must exactly match the requested range (fully contained)
					return overlap != null &&
							overlap.getStart().equals(requestedStartTime) &&
							overlap.getEnd().equals(requestedEndTime);
				});

		if (!isAvailable) {
			throw new IllegalArgumentException(
					"Requested time range is not available for booking. " +
							"Start: " + requestedStartTime + ", End: " + requestedEndTime);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<BookingSuggestion> calculateBookingSuggestion(
			SessionType sessionType,
			LocalDate suggestedDate,
			String timezone) {
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
							"Today start time: " + todayStartInstant.atZone(zoneId));
		}

		// Get booking settings for slot interval
		BookingSettings settings = bookingSettingsRepository.mustFindTopByOrderByIdAsc();

		// Calculate available time ranges
		List<TimeRange> allAvailableRanges = calculateAvailableTimeRanges(
				dayStartInstant, dayEndInstant, suggestedDate, zoneId, settings);

		List<BookingSuggestion> suggestions = new ArrayList<>();
		for (TimeRange availableRange : allAvailableRanges) {
			suggestions
					.addAll(generateSlots(availableRange, sessionType, settings.getBookingSlotsInterval()));
		}

		return suggestions;
	}

	/**
	 * Finds availability rules that match a specific time range.
	 * A rule matches if:
	 * 1. The time range overlaps with the rule's valid period
	 * 2. The day of week matches
	 * 3. The time is within the rule's available hours
	 */
	private List<AvailabilityRule> findMatchingRulesForTime(Instant startTime, Instant endTime) {
		List<AvailabilityRule> activeRules = repository.findByRuleStatus(AvailabilityRule.RuleStatus.ACTIVE);

		return activeRules.stream()
				.filter(rule -> {
					// Check if rule is valid during the booking time period (both start and end
					// times)
					if (!isRuleTimeFit(rule, startTime, endTime)) {
						return false;
					}

					ZoneId ruleZoneId = ZoneId.of(rule.getTimezone());

					// Convert booking startTime to LocalTime and DayOfWeek using rule's timezone
					LocalTime bookingStartTime = startTime.atZone(ruleZoneId).toLocalTime();
					LocalTime bookingEndTime = endTime.atZone(ruleZoneId).toLocalTime();
					DayOfWeek bookingDayOfWeek = startTime.atZone(ruleZoneId).getDayOfWeek();

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
	}

	/**
	 * Finds availability rules that match a specific day.
	 * A rule matches if:
	 * 1. The rule overlaps with the day
	 * 2. The day of week matches
	 */
	private List<AvailabilityRule> findMatchingRulesForDay(
			Instant dayStartInstant, Instant dayEndInstant, LocalDate suggestedDate, ZoneId zoneId) {
		List<AvailabilityRule> activeRules = repository.findByRuleStatus(AvailabilityRule.RuleStatus.ACTIVE);

		return activeRules.stream()
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
	}

	/**
	 * Calculates available time ranges for a given day by considering availability
	 * rules,
	 * overrides, and existing bookings.
	 */
	private List<TimeRange> calculateAvailableTimeRanges(
			Instant dayStartInstant,
			Instant dayEndInstant,
			LocalDate suggestedDate,
			ZoneId zoneId,
			BookingSettings settings) {
		// Find rules that match the day
		List<AvailabilityRule> matchingRules = findMatchingRulesForDay(
				dayStartInstant, dayEndInstant, suggestedDate, zoneId);

		// Calculate minimum start time (now + bookingFirstSlotInterval)
		// This ensures sessions cannot be booked too soon (e.g., not within 5 minutes,
		// or not within 2 days)
		Instant minimumStartTime = Instant.now().plusSeconds(settings.getBookingFirstSlotInterval() * 60L);

		// Get unavailable overrides that reduce availability
		List<AvailabilityOverride> unavailableOverrides = overrideRepository.findOverlappingUnavailableOverrides(
				dayStartInstant, dayEndInstant, OverrideStatus.ACTIVE);

		// Collect working hours from all matching rules and find overlap with client's
		// day
		// Note: Different rules' working hours don't overlap, so we process each
		// separately
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

			// Find overlap between client's day and rule's working hours (filter past
			// intervals)
			TimeRange ruleOverlap = findFutureOverlap(
					new TimeRange(dayStartInstant, dayEndInstant),
					new TimeRange(ruleWorkingStartInstant, ruleWorkingEndInstant),
					minimumStartTime);

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
			// Calculate overlap between override and client's day (filter past intervals if
			// today)
			TimeRange overrideOverlap = findFutureOverlap(
					new TimeRange(dayStartInstant, dayEndInstant),
					new TimeRange(override.getOverideStartInstant(), override.getOverrideEndInstant()),
					minimumStartTime);

			if (overrideOverlap != null) {
				allAvailableRanges.add(overrideOverlap);
			}
		}

		// Subtract booked time ranges from all available ranges
		allAvailableRanges = subtractBookedRangesFromAll(allAvailableRanges, dayStartInstant, dayEndInstant);

		return allAvailableRanges;
	}

	private boolean isRuleTimeFit(AvailabilityRule rule, Instant bookingStartTime, Instant bookingEndTime) {
		Instant ruleStart = rule.getRuleStartInstant();
		Instant ruleEnd = rule.getRuleEndInstant();

		// Check if booking start time is within rule's valid period [ruleStart,
		// ruleEnd]
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

	private boolean isTimeWithinAvailableHours(AvailabilityRule rule, LocalTime bookingStartTime,
			LocalTime bookingEndTime) {
		LocalTime availableStart = rule.getAvailableStartTime();
		LocalTime availableEnd = rule.getAvailableEndTime();

		// Check if booking start time is within available hours [availableStart,
		// availableEnd]
		boolean startAfterOrEqual = bookingStartTime.isAfter(availableStart) || bookingStartTime.equals(availableStart);
		boolean startBeforeOrEqual = bookingStartTime.isBefore(availableEnd) || bookingStartTime.equals(availableEnd);
		boolean startWithinHours = startAfterOrEqual && startBeforeOrEqual;

		// Check if booking end time is within available hours [availableStart,
		// availableEnd]
		boolean endAfterOrEqual = bookingEndTime.isAfter(availableStart) || bookingEndTime.equals(availableStart);
		boolean endBeforeOrEqual = bookingEndTime.isBefore(availableEnd) || bookingEndTime.equals(availableEnd);
		boolean endWithinHours = endAfterOrEqual && endBeforeOrEqual;

		return startWithinHours && endWithinHours;
	}

	private List<BookingSuggestion> generateSlots(TimeRange range, SessionType sessionType,
			int slotIntervalMinutes) {

		List<BookingSuggestion> slots = new ArrayList<>();
		Instant currentStart = range.getStart();

		while (true) {

			Instant slotEnd = currentStart.plusSeconds(sessionType.getDurationMinutes() * 60L);
			Instant slotWithBufferEnd = slotEnd.plusSeconds(sessionType.getBufferMinutes() * 60L);

			// Check if slot end exceeds the range end
			if (slotWithBufferEnd.isAfter(range.getEnd())) {
				break;
			}

			BookingSuggestion suggestion = new BookingSuggestion();
			suggestion.setStartTime(currentStart);
			suggestion.setEndTime(slotEnd);
			suggestion.setStartTimeInstant(currentStart);
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

		// If minimumStartTime is provided (for today's bookings), ensure overlap starts
		// after it
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
	 * Subtracts booked time ranges from all available time ranges.
	 * Returns a list of remaining available time ranges after excluding booked
	 * periods.
	 */
	private List<TimeRange> subtractBookedRangesFromAll(
			List<TimeRange> availableRanges,
			Instant dayStartInstant,
			Instant dayEndInstant) {
		// Load all PENDING_APPROVAL and CONFIRMED bookings within the day range
		List<Booking> existingBookings = bookingRepository.findBookingsByStatusAndTimeRange(
				List.of(BookingStatus.PENDING_APPROVAL, BookingStatus.CONFIRMED),
				dayStartInstant,
				dayEndInstant);

		if (existingBookings.isEmpty()) {
			return availableRanges;
		}

		// Subtract booked time ranges from all available ranges
		List<TimeRange> bookedRanges = existingBookings.stream()
				.map(booking -> new TimeRange(booking.getStartTime(), booking.getEndTime()))
				.collect(Collectors.toList());

		List<TimeRange> result = new ArrayList<>(availableRanges);

		// Apply each booked range to all current available ranges
		for (TimeRange bookedRange : bookedRanges) {
			List<TimeRange> newResult = new ArrayList<>();
			for (TimeRange currentRange : result) {
				// Subtract booked range from current range
				List<TimeRange> remaining = subtractRange(currentRange, bookedRange);
				newResult.addAll(remaining);
			}
			result = newResult;
		}

		return result;
	}

	/**
	 * Subtracts unavailable override ranges from all available time ranges.
	 * Returns a list of remaining available time ranges after excluding unavailable
	 * periods.
	 */
	private List<TimeRange> subtractUnavailableRangesFromAll(
			List<TimeRange> availableRanges,
			List<AvailabilityOverride> unavailableOverrides) {
		if (unavailableOverrides.isEmpty()) {
			return availableRanges;
		}

		List<TimeRange> unavailableRanges = unavailableOverrides.stream()
				.map(override -> new TimeRange(
						override.getOverideStartInstant(),
						override.getOverrideEndInstant()))
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

		// If overlap completely covers original (overlap equals original), return empty
		// list
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
