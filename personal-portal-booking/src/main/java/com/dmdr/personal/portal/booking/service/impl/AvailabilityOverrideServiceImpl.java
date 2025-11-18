package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.availability.override.AvailabilityOverrideResponse;
import com.dmdr.personal.portal.booking.dto.availability.override.CreateAvailabilityOverrideRequest;
import com.dmdr.personal.portal.booking.dto.availability.override.UpdateAvailabilityOverrideRequest;
import com.dmdr.personal.portal.booking.model.AvailabilityOverride;
import com.dmdr.personal.portal.booking.repository.AvailabilityOverrideRepository;
import com.dmdr.personal.portal.booking.service.AvailabilityOverrideService;
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
		AvailabilityOverride entity = new AvailabilityOverride();
		entity.setOverrideDate(request.getOverrideDate());
		entity.setStartTime(request.getStartTime());
		entity.setEndTime(request.getEndTime());
		entity.setAvailable(request.isAvailable());
		AvailabilityOverride saved = repository.save(entity);
		return toResponse(saved);
	}

	@Override
	@Transactional
	public AvailabilityOverrideResponse update(Long id, UpdateAvailabilityOverrideRequest request) {
		AvailabilityOverride entity = repository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("AvailabilityOverride not found: " + id));
		entity.setOverrideDate(request.getOverrideDate());
		entity.setStartTime(request.getStartTime());
		entity.setEndTime(request.getEndTime());
		entity.setAvailable(request.isAvailable());
		AvailabilityOverride saved = repository.save(entity);
		return toResponse(saved);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		repository.deleteById(id);
	}

	private static AvailabilityOverrideResponse toResponse(AvailabilityOverride entity) {
		AvailabilityOverrideResponse resp = new AvailabilityOverrideResponse();
		resp.setId(entity.getId());
		resp.setOverrideDate(entity.getOverrideDate());
		resp.setStartTime(entity.getStartTime());
		resp.setEndTime(entity.getEndTime());
		resp.setAvailable(entity.isAvailable());
		return resp;
	}
}

