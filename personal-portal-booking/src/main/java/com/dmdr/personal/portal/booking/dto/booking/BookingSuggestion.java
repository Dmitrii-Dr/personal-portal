package com.dmdr.personal.portal.booking.dto.booking;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingSuggestion {
	private Instant startTime;
	private Instant endTime;
}

