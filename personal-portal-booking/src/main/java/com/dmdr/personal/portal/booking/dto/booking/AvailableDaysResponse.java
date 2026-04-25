package com.dmdr.personal.portal.booking.dto.booking;

import java.time.LocalDate;
import java.util.List;

import com.dmdr.personal.portal.core.model.TimezoneEntry;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AvailableDaysResponse {
	private List<LocalDate> days;
	private TimezoneEntry timezone;
	private Long sessionTypeId;
}
