package com.dmdr.personal.portal.booking.dto.booking;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBookingAdminRequest extends CreateBookingRequest {
	
	@NotNull
	private UUID userId;
}

