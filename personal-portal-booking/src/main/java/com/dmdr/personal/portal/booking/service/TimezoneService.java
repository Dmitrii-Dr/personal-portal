package com.dmdr.personal.portal.booking.service;

import com.dmdr.personal.portal.booking.dto.TimezoneResponse;
import java.util.List;

public interface TimezoneService {
	List<TimezoneResponse> getAllTimezones();
}

