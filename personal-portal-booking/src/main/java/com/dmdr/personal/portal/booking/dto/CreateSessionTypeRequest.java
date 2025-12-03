package com.dmdr.personal.portal.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
public class CreateSessionTypeRequest {
	@NotBlank
	@Size(max = 200)
	private String name;

	@Size(max = 2000)
	private String description;

	@NotNull
	@Min(1)
	private int durationMinutes;

	@NotNull
	@Min(0)
	private int bufferMinutes;

	private Map<String, BigDecimal> prices;
}

