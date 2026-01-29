package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.TimezoneResponse;
import com.dmdr.personal.portal.core.model.TimezoneEntry;
import com.dmdr.personal.portal.booking.service.TimezoneService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TimezoneServiceImpl implements TimezoneService {

	@Override
	@Transactional(readOnly = true)
	public List<TimezoneResponse> getAllTimezones() {
		List<TimezoneResponse> timezones = new ArrayList<>();

		for (TimezoneEntry entry : TimezoneEntry.values()) {
			TimezoneResponse response = new TimezoneResponse();
			response.setId(entry.getId());
			response.setDisplayName(entry.getDisplayName());
			response.setOffset(entry.getGmtOffset());
			timezones.add(response);
		}

		return timezones;
	}
}
