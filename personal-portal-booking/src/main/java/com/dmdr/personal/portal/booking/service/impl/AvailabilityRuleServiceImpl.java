package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.availability.rule.AvailabilityRuleResponse;
import com.dmdr.personal.portal.booking.dto.availability.rule.CreateAvailabilityRuleRequest;
import com.dmdr.personal.portal.booking.dto.availability.rule.UpdateAvailabilityRuleRequest;
import com.dmdr.personal.portal.booking.model.AvailabilityRule;
import com.dmdr.personal.portal.booking.repository.AvailabilityRuleRepository;
import com.dmdr.personal.portal.booking.service.AvailabilityRuleService;
import com.dmdr.personal.portal.booking.service.BookingSettingsService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityRuleServiceImpl implements AvailabilityRuleService {

	private final AvailabilityRuleRepository repository;
	private final AvailabilityRuleValidator validator;
	private final BookingSettingsService bookingSettingsService;

	public AvailabilityRuleServiceImpl(
		AvailabilityRuleRepository repository,
		AvailabilityRuleValidator validator,
		BookingSettingsService bookingSettingsService
	) {
		this.repository = repository;
		this.validator = validator;
		this.bookingSettingsService = bookingSettingsService;
	}

	@Override
	@Transactional(readOnly = true)
	public List<AvailabilityRuleResponse> getAll() {
		return repository.findAll().stream()
			.map(this::toResponse)
			.sorted(Comparator.comparing(AvailabilityRuleResponse::getRuleStartDate)
				.thenComparing(AvailabilityRuleResponse::getAvailableStartTime))
			.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<AvailabilityRuleResponse> getAllActive() {
		return repository.findByRuleStatus(AvailabilityRule.RuleStatus.ACTIVE).stream()
			.map(this::toResponse)
			.sorted(Comparator.comparing(AvailabilityRuleResponse::getRuleStartDate)
				.thenComparing(AvailabilityRuleResponse::getAvailableStartTime))
			.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public AvailabilityRuleResponse create(CreateAvailabilityRuleRequest request) {
		// Get timezone from BookingSettings
		String timezone = bookingSettingsService.getDefaultTimezone();
		ZoneId zoneId = ZoneId.of(timezone);

		// Transform LocalDate to LocalDateTime using availableStartTime and availableEndTime, then to Instant
		Instant ruleStartInstant = request.getRuleStartDate().atTime(request.getAvailableStartTime()).atZone(zoneId).toInstant();
		Instant ruleEndInstant = request.getRuleEndDate().atTime(request.getAvailableEndTime()).atZone(zoneId).toInstant();

		// Validate rule start time (both ruleStartInstant and Instant.now() are in UTC)
		validator.validateRuleStartTime(ruleStartInstant, timezone);

		// Validate overlapping rules (timezone and active rule overlap) - single DB query
		validator.validateOverlappingRules(request, ruleStartInstant, ruleEndInstant, timezone);

		TimezoneOffsetPair resolved = resolveTimezoneAndOffset(
			timezone,
			ruleStartInstant,
			ruleEndInstant
		);

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

		// Use existing timezone from entity (timezone cannot be updated)
		String timezone = entity.getTimezone();
		ZoneId zoneId = ZoneId.of(timezone);

		// Transform LocalDate to LocalDateTime using availableStartTime and availableEndTime, then to Instant
		Instant ruleStartInstant = request.getRuleStartDate().atTime(request.getAvailableStartTime()).atZone(zoneId).toInstant();
		Instant ruleEndInstant = request.getRuleEndDate().atTime(request.getAvailableEndTime()).atZone(zoneId).toInstant();

		// Validate overlapping rules (timezone and active rule overlap) - single DB query
		validator.validateOverlappingRules(request, ruleStartInstant, ruleEndInstant, timezone, id);

		TimezoneOffsetPair resolved = resolveTimezoneAndOffset(
			timezone,
			ruleStartInstant,
			ruleEndInstant
		);

		entity.setDaysOfWeekAsInt(convertToIntArray(request.getDaysOfWeek()));
		entity.setAvailableStartTime(request.getAvailableStartTime());
		entity.setAvailableEndTime(request.getAvailableEndTime());
		entity.setRuleStartInstant(ruleStartInstant);
		entity.setRuleEndInstant(ruleEndInstant);
		// timezone is preserved from existing entity (cannot be updated)
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
		String timezone = entity.getTimezone();
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
		// Calculate offset from timezone at ruleStartInstant
		String calculatedOffset = calculateOffsetFromTimezone(timezone, ruleStartInstant);

		// Validate that offset is consistent across rule period
		validator.validateOffsetConsistency(timezone, ruleStartInstant, ruleEndInstant);

		return new TimezoneOffsetPair(timezone, calculatedOffset);
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


	private static class TimezoneOffsetPair {
		final String timezone;
		final String utcOffset;

		TimezoneOffsetPair(String timezone, String utcOffset) {
			this.timezone = timezone;
			this.utcOffset = utcOffset;
		}
	}
}


