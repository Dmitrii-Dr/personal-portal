package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.availability.rule.AvailabilityRuleResponse;
import com.dmdr.personal.portal.booking.dto.availability.rule.CreateAvailabilityRuleRequest;
import com.dmdr.personal.portal.booking.dto.availability.rule.UpdateAvailabilityRuleRequest;
import com.dmdr.personal.portal.booking.model.AvailabilityRule;
import com.dmdr.personal.portal.booking.model.BookingSettings;
import com.dmdr.personal.portal.booking.repository.AvailabilityRuleRepository;
import com.dmdr.personal.portal.booking.repository.BookingSettingsRepository;
import com.dmdr.personal.portal.booking.service.AvailabilityRuleService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityRuleServiceImpl implements AvailabilityRuleService {

	private final AvailabilityRuleRepository repository;
	private final BookingSettingsRepository bookingSettingsRepository;

	public AvailabilityRuleServiceImpl(
		AvailabilityRuleRepository repository,
		BookingSettingsRepository bookingSettingsRepository
	) {
		this.repository = repository;
		this.bookingSettingsRepository = bookingSettingsRepository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<AvailabilityRuleResponse> getAll() {
		return repository.findAll().stream()
			.map(this::toResponse)
			.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public AvailabilityRuleResponse create(CreateAvailabilityRuleRequest request) {
		// Resolve timezone (from request or default)
		String timezone = request.getTimezone() != null ? request.getTimezone() : getDefaultTimezone();
		ZoneId zoneId = ZoneId.of(timezone);

		// Transform LocalDate to LocalDateTime (start at 00:00:00, end at 23:59:59) then to Instant
		Instant ruleStartInstant = request.getRuleStartDate().atStartOfDay().atZone(zoneId).toInstant();
		Instant ruleEndInstant = request.getRuleEndDate().atTime(23, 59, 59).atZone(zoneId).toInstant();

		// Validate rule start time
		Instant now = Instant.now();
		if (ruleStartInstant.isBefore(now)) {
			throw new IllegalArgumentException("Rule start time cannot be before the current time");
		}

		TimezoneOffsetPair resolved = resolveTimezoneAndOffset(
			timezone,
			ruleStartInstant,
			ruleEndInstant
		);

		// Validate overlap with existing ACTIVE rules if creating an ACTIVE rule
		if (request.getRuleStatus() == AvailabilityRule.RuleStatus.ACTIVE) {
			validateNoOverlapWithActiveRules(
				ruleStartInstant,
				ruleEndInstant,
				request.getDaysOfWeek(),
				request.getAvailableStartTime(),
				request.getAvailableEndTime(),
				null // No rule to exclude when creating
			);
		}

		AvailabilityRule entity = new AvailabilityRule();
		entity.setDaysOfWeekAsInt(convertToIntArray(request.getDaysOfWeek()));
		entity.setAvailableStartTime(request.getAvailableStartTime());
		entity.setAvailableEndTime(request.getAvailableEndTime());
		entity.setRuleStartInstant(ruleStartInstant);
		entity.setRuleEndInstant(ruleEndInstant);
		entity.setTimezone(resolved.timezone);
		entity.setUtcOffset(resolved.utcOffset);
		entity.setRuleStatus(request.getRuleStatus());
		AvailabilityRule saved = repository.save(entity);
		return toResponse(saved);
	}

	@Override
	@Transactional
	public AvailabilityRuleResponse update(Long id, UpdateAvailabilityRuleRequest request) {
		AvailabilityRule entity = repository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("AvailabilityRule not found: " + id));

		// Resolve timezone (from request or default)
		String timezone = request.getTimezone() != null ? request.getTimezone() : getDefaultTimezone();
		ZoneId zoneId = ZoneId.of(timezone);

		// Transform LocalDate to LocalDateTime (start at 00:00:00, end at 23:59:59) then to Instant
		Instant ruleStartInstant = request.getRuleStartDate().atStartOfDay().atZone(zoneId).toInstant();
		Instant ruleEndInstant = request.getRuleEndDate().atTime(23, 59, 59).atZone(zoneId).toInstant();

		TimezoneOffsetPair resolved = resolveTimezoneAndOffset(
			timezone,
			ruleStartInstant,
			ruleEndInstant
		);

		// Validate overlap with existing ACTIVE rules if updating to ACTIVE status
		if (request.getRuleStatus() == AvailabilityRule.RuleStatus.ACTIVE) {
			validateNoOverlapWithActiveRules(
				ruleStartInstant,
				ruleEndInstant,
				request.getDaysOfWeek(),
				request.getAvailableStartTime(),
				request.getAvailableEndTime(),
				id // Exclude current rule from overlap check
			);
		}

		entity.setDaysOfWeekAsInt(convertToIntArray(request.getDaysOfWeek()));
		entity.setAvailableStartTime(request.getAvailableStartTime());
		entity.setAvailableEndTime(request.getAvailableEndTime());
		entity.setRuleStartInstant(ruleStartInstant);
		entity.setRuleEndInstant(ruleEndInstant);
		entity.setTimezone(resolved.timezone);
		entity.setUtcOffset(resolved.utcOffset);
		entity.setRuleStatus(request.getRuleStatus());
		AvailabilityRule saved = repository.save(entity);
		return toResponse(saved);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		repository.deleteById(id);
	}

	private AvailabilityRuleResponse toResponse(AvailabilityRule entity) {
		AvailabilityRuleResponse resp = new AvailabilityRuleResponse();
		resp.setId(entity.getId());
		resp.setDaysOfWeek(convertToDayOfWeekList(entity.getDaysOfWeekAsInt()));
		resp.setAvailableStartTime(entity.getAvailableStartTime());
		resp.setAvailableEndTime(entity.getAvailableEndTime());
		
		// Convert Instant back to LocalDate using entity's timezone
		String timezone = entity.getTimezone() != null ? entity.getTimezone() : getDefaultTimezone();
		ZoneId zoneId = ZoneId.of(timezone);
		resp.setRuleStartDate(entity.getRuleStartInstant().atZone(zoneId).toLocalDate());
		resp.setRuleEndDate(entity.getRuleEndInstant().atZone(zoneId).toLocalDate());
		
		resp.setTimezone(entity.getTimezone());
		resp.setUtcOffset(entity.getUtcOffset());
		resp.setRuleStatus(entity.getRuleStatus());
		return resp;
	}

	// Convert List<DayOfWeek> to int[] where Monday = 1, Sunday = 7
	private static int[] convertToIntArray(List<DayOfWeek> daysOfWeek) {
		return daysOfWeek.stream()
			.mapToInt(DayOfWeek::getValue)
			.toArray();
	}

	// Convert int[] to List<DayOfWeek> where Monday = 1, Sunday = 7
	private static List<DayOfWeek> convertToDayOfWeekList(int[] daysOfWeekAsInt) {
		return Arrays.stream(daysOfWeekAsInt)
			.mapToObj(DayOfWeek::of)
			.collect(Collectors.toList());
	}

	private TimezoneOffsetPair resolveTimezoneAndOffset(
		String timezone,
		Instant ruleStartInstant,
		Instant ruleEndInstant
	) {
		// If timezone is null, use default timezone
		String finalTimezone = timezone != null ? timezone : getDefaultTimezone();

		// Calculate offset from timezone at ruleStartInstant
		String calculatedOffset = calculateOffsetFromTimezone(finalTimezone, ruleStartInstant);

		// Validate that offset is consistent across rule period
		validateOffsetConsistency(finalTimezone, ruleStartInstant, ruleEndInstant);

		return new TimezoneOffsetPair(finalTimezone, calculatedOffset);
	}

	private String getDefaultTimezone() {
		return bookingSettingsRepository.findTopByOrderByIdAsc()
			.map(BookingSettings::getDefaultTimezone)
			.orElse("UTC");
	}

	private static String calculateOffsetFromTimezone(String timezone, Instant referenceTime) {
		try {
			ZoneId zoneId = ZoneId.of(timezone);
			ZoneOffset offset = zoneId.getRules().getOffset(referenceTime);
			return offset.toString();
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid timezone: " + e.getMessage(), e);
		}
	}

	private static void validateOffsetConsistency(String timezone, Instant ruleStartInstant, Instant ruleEndInstant) {
		try {
			ZoneId zoneId = ZoneId.of(timezone);
			ZoneOffset offsetAtStart = zoneId.getRules().getOffset(ruleStartInstant);
			ZoneOffset offsetAtEnd = zoneId.getRules().getOffset(ruleEndInstant);

			if (!offsetAtStart.equals(offsetAtEnd)) {
				throw new IllegalArgumentException(
					"The rule period spans across a Daylight Saving Time (DST) change. " +
						"Timezone '" + timezone + "' has UTC offset " + offsetAtStart + " at ruleStartInstant " +
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

	private void validateNoOverlapWithActiveRules(
		Instant ruleStartInstant,
		Instant ruleEndInstant,
		List<DayOfWeek> daysOfWeek,
		LocalTime availableStartTime,
		LocalTime availableEndTime,
		Long excludeRuleId
	) {
		int[] daysOfWeekAsInt = convertToIntArray(daysOfWeek);
		List<AvailabilityRule> activeRules = repository.findByRuleStatus(AvailabilityRule.RuleStatus.ACTIVE);

		for (AvailabilityRule existingRule : activeRules) {
			// Skip the rule being updated (exclude it from overlap check)
			if (excludeRuleId != null && existingRule.getId().equals(excludeRuleId)) {
				continue;
			}

			if (overlaps(ruleStartInstant, ruleEndInstant, daysOfWeekAsInt, availableStartTime, availableEndTime, existingRule)) {
				String action = excludeRuleId != null ? "update" : "create";
				throw new IllegalArgumentException(
					"Cannot " + action + " ACTIVE rule: it overlaps with existing ACTIVE rule (ID: " + existingRule.getId() + "). " +
					"Overlap is defined by rule period, days of week, and available time range."
				);
			}
		}
	}

	private boolean overlaps(
		Instant ruleStartInstant,
		Instant ruleEndInstant,
		int[] daysOfWeekAsInt,
		LocalTime availableStartTime,
		LocalTime availableEndTime,
		AvailabilityRule existingRule
	) {
		// Check time period overlap: ruleStartInstant < other.ruleEndInstant AND ruleEndInstant > other.ruleStartInstant
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

		// Check time range overlap: availableStartTime < other.availableEndTime AND availableEndTime > other.availableStartTime
		boolean timeRangeOverlaps = availableStartTime.isBefore(existingRule.getAvailableEndTime()) &&
			availableEndTime.isAfter(existingRule.getAvailableStartTime());

		return timeRangeOverlaps;
	}

	private static boolean containsDay(int[] daysOfWeekAsInt, int dayValue) {
		return Arrays.stream(daysOfWeekAsInt)
			.anyMatch(day -> day == dayValue);
	}

	private static class TimezoneOffsetPair {
		final String timezone;
		final String utcOffset;

		TimezoneOffsetPair(String timezone, String utcOffset) {
			this.timezone = timezone;
			this.utcOffset = utcOffset;
		}
	}
}


