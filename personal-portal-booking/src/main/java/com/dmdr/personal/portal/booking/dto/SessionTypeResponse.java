package com.dmdr.personal.portal.booking.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionTypeResponse {
	private Long id;
	private String name;
	private String description;
	private int durationMinutes;
	private int bufferMinutes;
}

