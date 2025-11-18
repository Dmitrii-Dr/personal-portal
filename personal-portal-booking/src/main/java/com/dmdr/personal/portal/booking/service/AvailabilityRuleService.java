package com.dmdr.personal.portal.booking.service;

import com.dmdr.personal.portal.booking.dto.availability.rule.AvailabilityRuleResponse;
import com.dmdr.personal.portal.booking.dto.availability.rule.CreateAvailabilityRuleRequest;
import com.dmdr.personal.portal.booking.dto.availability.rule.UpdateAvailabilityRuleRequest;

import java.util.List;

public interface AvailabilityRuleService {
	List<AvailabilityRuleResponse> getAll();
	AvailabilityRuleResponse create(CreateAvailabilityRuleRequest request);
	AvailabilityRuleResponse update(Long id, UpdateAvailabilityRuleRequest request);
	void delete(Long id);
}


