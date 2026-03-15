package com.dmdr.personal.portal.booking.dto.booking;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class UpdateBookingAdminRequest {
	@NotNull
	private UUID userId;

	@NotNull
	private Long id;

	@NotNull
	private Instant startTime;

	@Size(max = 2000)
	private String clientMessage;
}

