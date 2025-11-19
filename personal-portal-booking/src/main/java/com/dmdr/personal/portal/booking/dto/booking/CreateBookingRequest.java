package com.dmdr.personal.portal.booking.dto.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBookingRequest {
	@NotNull
	private Long sessionTypeId;

	@NotNull
	@Future
	private Instant startTimeInstant;

	@Size(max = 2000)
	private String clientMessage;
}

