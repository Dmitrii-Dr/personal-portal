package com.dmdr.personal.portal.booking.service;

import com.dmdr.personal.portal.booking.dto.availability.override.AvailabilityOverrideResponse;
import com.dmdr.personal.portal.booking.dto.availability.override.CreateAvailabilityOverrideRequest;
import com.dmdr.personal.portal.booking.dto.availability.override.UpdateAvailabilityOverrideRequest;

import java.util.List;

public interface AvailabilityOverrideService {
	List<AvailabilityOverrideResponse> getAll();
	AvailabilityOverrideResponse create(CreateAvailabilityOverrideRequest request);
	AvailabilityOverrideResponse update(Long id, UpdateAvailabilityOverrideRequest request);
	void delete(Long id);
}

