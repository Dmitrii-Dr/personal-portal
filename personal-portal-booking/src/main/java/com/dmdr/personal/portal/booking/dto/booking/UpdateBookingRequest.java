package com.dmdr.personal.portal.booking.dto.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBookingRequest {
	@NotNull
	private Long id;

	@NotNull
	@Future
	private Instant startTime;

	@Size(max = 2000)
	private String clientMessage;
}

