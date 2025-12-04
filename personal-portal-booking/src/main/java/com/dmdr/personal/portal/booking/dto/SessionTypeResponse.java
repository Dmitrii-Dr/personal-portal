package com.dmdr.personal.portal.booking.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
public class SessionTypeResponse {
	private Long id;
	private String name;
	private String description;
	private int durationMinutes;
	private int bufferMinutes;
	private Map<String, BigDecimal> prices;
	private boolean active;
}

