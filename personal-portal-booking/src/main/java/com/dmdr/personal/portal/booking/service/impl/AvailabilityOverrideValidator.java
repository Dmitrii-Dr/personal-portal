package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.model.AvailabilityRule;
import com.dmdr.personal.portal.booking.repository.AvailabilityRuleRepository;
import com.dmdr.personal.portal.core.model.TimezoneEntry;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AvailabilityOverrideValidator {

	private final AvailabilityRuleRepository repository;

	public AvailabilityOverrideValidator(AvailabilityRuleRepository repository) {
		this.repository = repository;
	}

	public void validateOverrideStartTime(Instant overrideStartInstant, TimezoneEntry timezone) {
		Instant now = Instant.now();
		if (overrideStartInstant.isBefore(now)) {
			ZoneId zoneId = ZoneId.of(timezone.getGmtOffset());
			throw new IllegalArgumentException(
					"Override start time cannot be before the current time. " +
							" Override start time: " + overrideStartInstant.atZone(zoneId)  +
							" Current time: " + now.atZone(zoneId));
		}
	}

	/**
	 * Validates override consistency with availability rules.
	 * 
	 * @param overrideStartInstant Override start time
	 * @param overrideEndInstant   Override end time
	 * @param isAvailable          true if override extends availability, false if
	 *                             it reduces availability
	 * @param overrideTimezone     Timezone of the override (must match rule
	 *                             timezone)
	 * @throws IllegalArgumentException if validation fails
	 */
	public void validateOverrideConsistencyWithRules(
			Instant overrideStartInstant,
			Instant overrideEndInstant,
			boolean isAvailable,
			TimezoneEntry overrideTimezone) {
		// Validate timezone consistency against ALL non-archived rules
		validateTimezoneConsistency(overrideTimezone);

		// Find all ACTIVE rules that overlap with the override time period
		List<AvailabilityRule> overlappingActiveRules = repository.findActiveOverlappingRules(
				overrideStartInstant,
				overrideEndInstant,
				AvailabilityRule.RuleStatus.ACTIVE);

		// Filter rules that actually overlap considering day of week and time range
		List<AvailabilityRule> trulyOverlappingRules = overlappingActiveRules.stream()
				.filter(rule -> overlapsWithOverride(rule, overrideStartInstant, overrideEndInstant, overrideTimezone))
				.toList();

		if (isAvailable) {
			// If override extends availability (isAvailable = true), there must be NO
			// overlapping rules
			if (!trulyOverlappingRules.isEmpty()) {
				throw new IllegalArgumentException(
						"Cannot create override with isAvailable=true: it overlaps with " +
								trulyOverlappingRules.size() + " ACTIVE availability rule(s). " +
								"Overrides that extend availability cannot overlap with existing working hours.");
			}
		} else {
			// If override reduces availability (isAvailable = false), there must be AT
			// LEAST ONE overlapping rule
			if (trulyOverlappingRules.isEmpty()) {
				throw new IllegalArgumentException(
						"Cannot create override with isAvailable=false: no ACTIVE availability rule found " +
								"for the specified time period. Overrides that reduce availability must overlap " +
								"with existing working hours.");
			}
		}
	}

	/**
	 * Validates that all non-archived rules have the same timezone as the override.
	 * 
	 * @param overrideTimezone Timezone of the override
	 * @throws IllegalArgumentException if any non-archived rule has a different
	 *                                  timezone
	 */
	private void validateTimezoneConsistency(TimezoneEntry overrideTimezone) {
		List<AvailabilityRule> allNonArchivedRules = repository.findAllNonArchivedRules(
				AvailabilityRule.RuleStatus.ARCHIVED, null);

		for (AvailabilityRule existingRule : allNonArchivedRules) {
			Integer existingTimezoneId = existingRule.getTimezoneId();
			TimezoneEntry existingTimezone = existingTimezoneId != null
					? TimezoneEntry.getById(existingTimezoneId)
					: null;

			boolean timezonesDiffer = existingTimezone != overrideTimezone;

			if (timezonesDiffer) {
				throw new IllegalArgumentException(
						"Cannot create or update override: there is an existing non-archived rule (ID: "
								+ existingRule.getId() +
								", Status: " + existingRule.getRuleStatus() + ") in a different timezone. " +
								"Existing rule timezone: "
								+ (existingTimezone != null ? "'" + existingTimezone + "'" : "null") + ", " +
								"Override timezone: '" + overrideTimezone + "'. " +
								"All non-archived rules and overrides must use the same timezone.");
			}
		}
	}

	/**
	 * Checks if a rule truly overlaps with an override considering:
	 * 1. Time period overlap (already checked by repository query)
	 * 2. Day of week overlap
	 * 3. Time range overlap
	 */
	private boolean overlapsWithOverride(
			AvailabilityRule rule,
			Instant overrideStartInstant,
			Instant overrideEndInstant,
			TimezoneEntry overrideTimezone) {
		ZoneId overrideZoneId = ZoneId.of(overrideTimezone.getGmtOffset());
		String ruleTimezone = TimezoneEntry.getById(rule.getTimezoneId()).getGmtOffset();

		// Validate timezone consistency - all rules and overrides must be in the same
		// timezone
		// (This is a defensive check; timezone consistency is already validated against
		// all non-archived rules)
		if (overrideTimezone.getId() != rule.getTimezoneId()) {
			throw new IllegalArgumentException(
					"Rules and overrides must be in the same timezone");
		}

		// Check day of week overlap
		// Override must be within a single day in origin timezone (but can span two
		// days in UTC)
		ZonedDateTime overrideStartZoned = overrideStartInstant.atZone(overrideZoneId);
		ZonedDateTime overrideEndZoned = overrideEndInstant.atZone(overrideZoneId);

		DayOfWeek overrideStartDayOfWeek = overrideStartZoned.getDayOfWeek();
		DayOfWeek overrideEndDayOfWeek = overrideEndZoned.getDayOfWeek();

		// Validate that override is within a single day in origin timezone
		if (!overrideStartDayOfWeek.equals(overrideEndDayOfWeek)) {
			throw new IllegalArgumentException(
					"Override cannot span two days in origin timezone. " +
							"Start day: " + overrideStartDayOfWeek + ", End day: " + overrideEndDayOfWeek);
		}

		// Check if rule applies to this day of week
		boolean dayOverlaps = containsDay(rule.getDaysOfWeekAsInt(), overrideStartDayOfWeek.getValue());

		if (!dayOverlaps) {
			return false;
		}

		// Check time range overlap
		// Convert override instants to LocalTime in override's timezone (same as rule's
		// timezone)
		// Since override cannot span two days in origin timezone, overrideEndTime will
		// always be >= overrideStartTime
		LocalTime overrideStartTime = overrideStartZoned.toLocalTime();
		LocalTime overrideEndTime = overrideEndZoned.toLocalTime();

		LocalTime ruleStartTime = rule.getAvailableStartTime();
		LocalTime ruleEndTime = rule.getAvailableEndTime();

		// Check if time ranges overlap: overrideStartTime < ruleEndTime AND
		// overrideEndTime > ruleStartTime
		boolean timeRangeOverlaps = overrideStartTime.isBefore(ruleEndTime) &&
				overrideEndTime.isAfter(ruleStartTime);

		return timeRangeOverlaps;
	}

	private static boolean containsDay(int[] daysOfWeekAsInt, int dayValue) {
		return Arrays.stream(daysOfWeekAsInt)
				.anyMatch(day -> day == dayValue);
	}
}
