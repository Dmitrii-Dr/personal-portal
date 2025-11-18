package com.dmdr.personal.portal.booking.dto.availability.override;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAvailabilityOverrideRequest {
	@NotNull
	private Long id;
	@NotNull
	private LocalDate overrideDate;
	@NotNull
	private LocalTime startTime;
	@NotNull
	private LocalTime endTime;
	@NotNull
	@JsonProperty("isAvailable")
	private boolean isAvailable;
	@NotNull
	private String timezone;
}

