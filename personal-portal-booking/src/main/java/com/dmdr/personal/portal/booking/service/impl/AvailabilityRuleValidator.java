package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.availability.rule.CreateAvailabilityRuleRequest;
import com.dmdr.personal.portal.booking.dto.availability.rule.UpdateAvailabilityRuleRequest;
import com.dmdr.personal.portal.booking.model.AvailabilityRule;
import com.dmdr.personal.portal.booking.repository.AvailabilityRuleRepository;
import com.dmdr.personal.portal.core.model.TimezoneEntry;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AvailabilityRuleValidator {

	private final AvailabilityRuleRepository repository;

	public AvailabilityRuleValidator(AvailabilityRuleRepository repository) {
		this.repository = repository;
	}

	public void validateRuleStartTime(Instant ruleStartInstant, TimezoneEntry timezone) {
		Instant now = Instant.now();
		if (ruleStartInstant.isBefore(now)) {
			// Both ruleStartInstant and now are in UTC, but provide timezone context in
			// error message
			ZoneId zoneId = ZoneId.of(timezone.getGmtOffset());
			throw new IllegalArgumentException(
					"Rule start time cannot be before the current time. " +
							"Rule start time: " + ruleStartInstant.atZone(zoneId) + " (" + timezone + "), " +
							"Current time: " + now.atZone(zoneId) + " (" + timezone + ")");
		}
	}

	public void validateOverlappingRules(
			CreateAvailabilityRuleRequest request,
			Instant ruleStartInstant,
			Instant ruleEndInstant,
			TimezoneEntry timezone) {
		RuleValidationContext context = RuleValidationContext.fromCreateRequest(
				request, ruleStartInstant, ruleEndInstant, timezone.getId());
		validateOverlappingRules(context);
	}

	public void validateOverlappingRules(
			UpdateAvailabilityRuleRequest request,
			Instant ruleStartInstant,
			Instant ruleEndInstant,
			Integer timezoneId,
			Long excludeRuleId) {
		RuleValidationContext context = RuleValidationContext.fromUpdateRequest(
				request, ruleStartInstant, ruleEndInstant, timezoneId, excludeRuleId);
		validateOverlappingRules(context);
	}

	private void validateOverlappingRules(RuleValidationContext context) {
		// Validate timezone consistency against ALL non-archived rules
		List<AvailabilityRule> allNonArchivedRules = repository.findAllNonArchivedRules(
				AvailabilityRule.RuleStatus.ARCHIVED, context.excludeRuleId);
		for (AvailabilityRule existingRule : allNonArchivedRules) {
			validateTimezoneConsistency(context.timezoneId, existingRule);
		}

		// Single DB query to get all overlapping rules for active rule overlap
		// validation
		List<AvailabilityRule> overlappingRules = repository.findOverlappingRules(
				context.ruleStartInstant, context.ruleEndInstant, context.excludeRuleId);

		int[] daysOfWeekAsInt = context.daysOfWeek != null ? convertToIntArray(context.daysOfWeek) : null;

		// Validate active rule overlap (only if creating/updating to ACTIVE status)
		for (AvailabilityRule existingRule : overlappingRules) {
			if (context.isActiveRuleValidation()
					&& existingRule.getRuleStatus() == AvailabilityRule.RuleStatus.ACTIVE) {
				validateActiveRuleOverlap(context, daysOfWeekAsInt, existingRule);
			}
		}
	}

	public void validateOffsetConsistency(TimezoneEntry timezone, Instant ruleStartInstant, Instant ruleEndInstant) {
		try {
			ZoneId zoneId = ZoneId.of(timezone.getGmtOffset());
			ZoneOffset offsetAtStart = zoneId.getRules().getOffset(ruleStartInstant);
			ZoneOffset offsetAtEnd = zoneId.getRules().getOffset(ruleEndInstant);

			if (!offsetAtStart.equals(offsetAtEnd)) {
				throw new IllegalArgumentException(
						"The rule period spans across a Daylight Saving Time (DST) change. " +
								"Timezone '" + timezone + "' has UTC offset " + offsetAtStart + " at ruleStartInstant "
								+
								"but " + offsetAtEnd + " at ruleEndInstant. " +
								"Please reduce the ruleEndInstant to stay within a single offset period.");
			}
		} catch (Exception e) {
			if (e instanceof IllegalArgumentException && e.getMessage().contains("DST")) {
				throw e;
			}
			throw new IllegalArgumentException("Invalid timezone: " + e.getMessage(), e);
		}
	}

	private void validateTimezoneConsistency(Integer timezoneId, AvailabilityRule existingRule) {
		Integer existingTimezoneId = existingRule.getTimezoneId();
		boolean timezonesDiffer = !timezoneId.equals(existingTimezoneId);

		if (timezonesDiffer) {
			String timezone = TimezoneEntry.getById(timezoneId).getGmtOffset();
			String existingTimezone = TimezoneEntry.getById(existingTimezoneId).getGmtOffset();
			throw new IllegalArgumentException(
					"Cannot create rule: there is an existing non-archived rule (ID: " + existingRule.getId() +
							", Status: " + existingRule.getRuleStatus() + ") in a different timezone. " +
							"Existing rule timezone: '" + existingTimezone + "', " +
							"New rule timezone: '" + timezone + "'. " +
							"All non-archived rules must use the same timezone.");
		}
	}

	private void validateActiveRuleOverlap(
			RuleValidationContext context,
			int[] daysOfWeekAsInt,
			AvailabilityRule existingRule) {
		if (overlaps(context.ruleStartInstant, context.ruleEndInstant, daysOfWeekAsInt,
				context.availableStartTime, context.availableEndTime, existingRule)) {
			String action = context.excludeRuleId != null ? "update" : "create";
			throw new IllegalArgumentException(
					"Cannot " + action + " ACTIVE rule: it overlaps with existing ACTIVE rule (ID: "
							+ existingRule.getId() + "). " +
							"Overlap is defined by rule period, days of week, and available time range.");
		}
	}

	private boolean overlaps(
			Instant ruleStartInstant,
			Instant ruleEndInstant,
			int[] daysOfWeekAsInt,
			LocalTime availableStartTime,
			LocalTime availableEndTime,
			AvailabilityRule existingRule) {
		// Check time period overlap: ruleStartInstant < other.ruleEndInstant AND
		// ruleEndInstant > other.ruleStartInstant
		boolean timePeriodOverlaps = ruleStartInstant.isBefore(existingRule.getRuleEndInstant()) &&
				ruleEndInstant.isAfter(existingRule.getRuleStartInstant());

		if (!timePeriodOverlaps) {
			return false;
		}

		// Check day overlap: any day in this.daysOfWeek is in other.daysOfWeek
		boolean dayOverlaps = Arrays.stream(daysOfWeekAsInt)
				.anyMatch(day -> containsDay(existingRule.getDaysOfWeekAsInt(), day));

		if (!dayOverlaps) {
			return false;
		}

		// Check time range overlap: availableStartTime < other.availableEndTime AND
		// availableEndTime > other.availableStartTime
		boolean timeRangeOverlaps = availableStartTime.isBefore(existingRule.getAvailableEndTime()) &&
				availableEndTime.isAfter(existingRule.getAvailableStartTime());

		return timeRangeOverlaps;
	}

	private static boolean containsDay(int[] daysOfWeekAsInt, int dayValue) {
		return Arrays.stream(daysOfWeekAsInt)
				.anyMatch(day -> day == dayValue);
	}

	private static int[] convertToIntArray(List<DayOfWeek> daysOfWeek) {
		return daysOfWeek.stream()
				.mapToInt(DayOfWeek::getValue)
				.toArray();
	}

	private static class RuleValidationContext {
		final Instant ruleStartInstant;
		final Instant ruleEndInstant;
		final Integer timezoneId;
		final List<DayOfWeek> daysOfWeek;
		final LocalTime availableStartTime;
		final LocalTime availableEndTime;
		final Long excludeRuleId;

		private RuleValidationContext(
				Instant ruleStartInstant,
				Instant ruleEndInstant,
				Integer timezoneId,
				List<DayOfWeek> daysOfWeek,
				LocalTime availableStartTime,
				LocalTime availableEndTime,
				Long excludeRuleId) {
			this.ruleStartInstant = ruleStartInstant;
			this.ruleEndInstant = ruleEndInstant;
			this.timezoneId = timezoneId;
			this.daysOfWeek = daysOfWeek;
			this.availableStartTime = availableStartTime;
			this.availableEndTime = availableEndTime;
			this.excludeRuleId = excludeRuleId;
		}

		public static RuleValidationContext fromCreateRequest(
				CreateAvailabilityRuleRequest request,
				Instant ruleStartInstant,
				Instant ruleEndInstant,
				Integer timezoneId) {
			boolean isActive = request.getRuleStatus() == AvailabilityRule.RuleStatus.ACTIVE;
			return new RuleValidationContext(
					ruleStartInstant,
					ruleEndInstant,
					timezoneId,
					isActive ? request.getDaysOfWeek() : null,
					isActive ? request.getAvailableStartTime() : null,
					isActive ? request.getAvailableEndTime() : null,
					null // No rule to exclude when creating
			);
		}

		public static RuleValidationContext fromUpdateRequest(
				UpdateAvailabilityRuleRequest request,
				Instant ruleStartInstant,
				Instant ruleEndInstant,
				Integer timezoneId,
				Long excludeRuleId) {
			boolean isActive = request.getRuleStatus() == AvailabilityRule.RuleStatus.ACTIVE;
			return new RuleValidationContext(
					ruleStartInstant,
					ruleEndInstant,
					timezoneId,
					isActive ? request.getDaysOfWeek() : null,
					isActive ? request.getAvailableStartTime() : null,
					isActive ? request.getAvailableEndTime() : null,
					excludeRuleId);
		}

		public boolean isActiveRuleValidation() {
			return daysOfWeek != null && availableStartTime != null && availableEndTime != null;
		}
	}
}
