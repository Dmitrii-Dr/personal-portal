package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.TimezoneResponse;
import com.dmdr.personal.portal.booking.service.TimezoneService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TimezoneServiceImpl implements TimezoneService {

	// Main timezones list
	private static final List<String> MAIN_TIMEZONES = List.of(
		"UTC",
		"America/New_York",
		"America/Chicago",
		"America/Denver",
		"America/Los_Angeles",
		"America/Phoenix",
		"America/Anchorage",
		"America/Honolulu",
		"Europe/London",
		"Europe/Paris",
		"Europe/Berlin",
		"Europe/Rome",
		"Europe/Madrid",
		"Europe/Moscow",
		"Asia/Tokyo",
		"Asia/Shanghai",
		"Asia/Hong_Kong",
		"Asia/Singapore",
		"Asia/Dubai",
		"Asia/Kolkata",
		"Australia/Sydney",
		"Australia/Melbourne",
		"Pacific/Auckland"
	);

	@Override
	@Transactional(readOnly = true)
	public List<TimezoneResponse> getAllTimezones() {
		Instant now = Instant.now();
		List<TimezoneResponse> timezones = new ArrayList<>();

		for (String timezoneId : MAIN_TIMEZONES) {
			ZoneId zoneId = ZoneId.of(timezoneId);
			ZoneOffset offset = zoneId.getRules().getOffset(now);

			TimezoneResponse response = new TimezoneResponse();
			response.setTimezone(timezoneId);
			response.setOffset(offset.toString());
			timezones.add(response);
		}

		return timezones;
	}
}

