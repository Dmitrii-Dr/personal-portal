package com.dmdr.personal.portal.booking.dto.booking;

import java.time.LocalDate;
import com.dmdr.personal.portal.core.model.TimezoneEntry;
import java.time.LocalTime;
import java.time.Instant;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingSuggestionsDto {
	private LocalDate date;
	private TimezoneEntry timezone;
	private String offset;
	private Long sessionTypeId;
	private List<Slot> slots;

	@Getter
	@Setter
	public static class Slot {
		private Instant startTimeInstant;
		//local date in given timezone may differ from UTC date (startTimeInstant)
		private LocalDate date;
		private LocalTime startTime;
		private LocalTime endTime;
	}
}
