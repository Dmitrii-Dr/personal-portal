package com.dmdr.personal.portal.booking.dto.booking;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class UpdateBookingAdminRequest extends UpdateBookingRequest {
	@NotNull
	private UUID userId;
}

