package com.dmdr.personal.portal.booking.dto.availability.override;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAvailabilityOverrideRequest {
	@NotNull
	private LocalDate overrideDate;
	@NotNull
	private LocalTime startTime;
	@NotNull
	private LocalTime endTime;
	@NotNull
	private boolean isAvailable;
}

