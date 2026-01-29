package com.dmdr.personal.portal.booking.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimezoneResponse {
	private int id;
	private String displayName;
	private String offset;
}
