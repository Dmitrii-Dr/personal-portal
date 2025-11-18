package com.dmdr.personal.portal.booking.dto.availability.override;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AvailabilityOverrideResponse {
	private Long id;
	private LocalDate overrideDate;
	private LocalTime startTime;
	private LocalTime endTime;
	private boolean isAvailable;
}

