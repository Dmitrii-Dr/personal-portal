package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.availability.override.AvailabilityOverrideResponse;
import com.dmdr.personal.portal.booking.dto.availability.override.CreateAvailabilityOverrideRequest;
import com.dmdr.personal.portal.booking.dto.availability.override.UpdateAvailabilityOverrideRequest;
import com.dmdr.personal.portal.booking.model.AvailabilityOverride;
import com.dmdr.personal.portal.booking.model.OverrideStatus;
import com.dmdr.personal.portal.booking.repository.AvailabilityOverrideRepository;
import com.dmdr.personal.portal.booking.service.AvailabilityOverrideService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityOverrideServiceImpl implements AvailabilityOverrideService {

	private final AvailabilityOverrideRepository repository;

	public AvailabilityOverrideServiceImpl(AvailabilityOverrideRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public List<AvailabilityOverrideResponse> getAll() {
		return repository.findAll().stream()
			.map(AvailabilityOverrideServiceImpl::toResponse)
			.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public AvailabilityOverrideResponse create(CreateAvailabilityOverrideRequest request) {
		ZoneId zoneId = ZoneId.of(request.getTimezone());
		
		validateOverrideDateNotInPast(request.getOverrideDate(), zoneId);
		
		LocalDateTime startDateTime = LocalDateTime.of(request.getOverrideDate(), request.getStartTime());
		LocalDateTime endDateTime = LocalDateTime.of(request.getOverrideDate(), request.getEndTime());
		
		ZonedDateTime startZoned = startDateTime.atZone(zoneId);
		ZonedDateTime endZoned = endDateTime.atZone(zoneId);
		
		Instant startInstant = startZoned.toInstant();
		Instant endInstant = endZoned.toInstant();
		
		validateOverrideDuration(startInstant, endInstant);
		validateNoOverlappingActiveOverrides(startInstant, endInstant, null);
		
		ZoneOffset utcOffset = zoneId.getRules().getOffset(startInstant);
		
		AvailabilityOverride entity = new AvailabilityOverride();
		entity.setOverideStartInstant(startInstant);
		entity.setOverrideEndInstant(endInstant);
		entity.setAvailable(request.isAvailable());
		entity.setTimezone(request.getTimezone());
		entity.setUtcOffset(utcOffset.toString());
		entity.setOverrideStatus(OverrideStatus.ACTIVE);
		AvailabilityOverride saved = repository.save(entity);
		return toResponse(saved);
	}

	@Override
	@Transactional
	public AvailabilityOverrideResponse update(Long id, UpdateAvailabilityOverrideRequest request) {
		AvailabilityOverride entity = repository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("AvailabilityOverride not found: " + id));
		
		ZoneId zoneId = request.getTimezone() != null 
			? ZoneId.of(request.getTimezone()) 
			: ZoneId.systemDefault();
		
		validateOverrideDateNotInPast(request.getOverrideDate(), zoneId);
		
		LocalDateTime startDateTime = LocalDateTime.of(request.getOverrideDate(), request.getStartTime());
		LocalDateTime endDateTime = LocalDateTime.of(request.getOverrideDate(), request.getEndTime());
		
		ZonedDateTime startZoned = startDateTime.atZone(zoneId);
		ZonedDateTime endZoned = endDateTime.atZone(zoneId);
		
		Instant startInstant = startZoned.toInstant();
		Instant endInstant = endZoned.toInstant();
		
		validateOverrideDuration(startInstant, endInstant);
		validateNoOverlappingActiveOverrides(startInstant, endInstant, id);
		
		ZoneOffset utcOffset = zoneId.getRules().getOffset(startInstant);
		
		entity.setOverideStartInstant(startInstant);
		entity.setOverrideEndInstant(endInstant);
		entity.setAvailable(request.isAvailable());
		entity.setTimezone(request.getTimezone());
		entity.setUtcOffset(utcOffset.toString());
		AvailabilityOverride saved = repository.save(entity);
		return toResponse(saved);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		repository.deleteById(id);
	}

	private static void validateOverrideDateNotInPast(LocalDate overrideDate, ZoneId zoneId) {
		LocalDate today = LocalDate.now(zoneId);
		if (overrideDate.isBefore(today)) {
			throw new IllegalArgumentException(
				"Override date cannot be in the past. Provided date: " + overrideDate + ", today: " + today);
		}
	}

	private void validateNoOverlappingActiveOverrides(Instant startInstant, Instant endInstant, Long excludeId) {
		List<AvailabilityOverride> overlapping = repository.findOverlappingActiveOverrides(
			startInstant, endInstant, OverrideStatus.ACTIVE, excludeId);
		
		if (!overlapping.isEmpty()) {
			throw new IllegalArgumentException(
				"Cannot create or update override: it overlaps with " + overlapping.size() + 
				" existing ACTIVE override(s). Overrides must not overlap.");
		}
	}

	private static void validateOverrideDuration(Instant startInstant, Instant endInstant) {
		if (endInstant.isBefore(startInstant)) {
			throw new IllegalArgumentException("Override end time must be after start time");
		}
		
		Duration duration = Duration.between(startInstant, endInstant);
		if (duration.toHours() > 24) {
			throw new IllegalArgumentException(
				"Override duration cannot exceed 24 hours. Duration: " + duration.toHours() + " hours");
		}
	}

	private static AvailabilityOverrideResponse toResponse(AvailabilityOverride entity) {
		ZoneId zoneId = entity.getTimezone() != null 
			? ZoneId.of(entity.getTimezone()) 
			: ZoneId.systemDefault();
		
		ZonedDateTime startZoned = entity.getOverideStartInstant().atZone(zoneId);
		ZonedDateTime endZoned = entity.getOverrideEndInstant().atZone(zoneId);
		
		AvailabilityOverrideResponse resp = new AvailabilityOverrideResponse();
		resp.setId(entity.getId());
		resp.setOverrideDate(startZoned.toLocalDate());
		resp.setStartTime(startZoned.toLocalTime());
		resp.setEndTime(endZoned.toLocalTime());
		resp.setAvailable(entity.isAvailable());
		resp.setTimezone(entity.getTimezone());
		resp.setUtcOffset(entity.getUtcOffset());
		resp.setStatus(entity.getOverrideStatus());
		return resp;
	}
}

