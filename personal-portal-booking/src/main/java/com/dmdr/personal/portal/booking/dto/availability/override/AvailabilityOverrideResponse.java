package com.dmdr.personal.portal.booking.dto.availability.override;

import com.dmdr.personal.portal.core.model.TimezoneEntry;
import com.dmdr.personal.portal.booking.model.OverrideStatus;
import com.fasterxml.jackson.annotation.JsonGetter;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AvailabilityOverrideResponse {
	private Long id;

	@Getter(AccessLevel.NONE)
	private boolean isAvailable;

	@JsonGetter("isAvailable")
	public boolean isAvailable() {
		return isAvailable;
	}

	private LocalDate overrideDate;
	private LocalTime startTime;
	private LocalTime endTime;

	private TimezoneEntry timezone;
	private String utcOffset;

	private OverrideStatus status;
}
