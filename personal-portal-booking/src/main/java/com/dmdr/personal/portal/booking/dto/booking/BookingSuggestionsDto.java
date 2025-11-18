package com.dmdr.personal.portal.booking.dto.booking;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingSuggestionsDto {
	private LocalDate date;
	private String timezone;
	private String offset;
	private Long sessionTypeId;
	private List<Slot> slots;

	@Getter
	@Setter
	public static class Slot {
		private LocalTime startTime;
		private LocalTime endTime;
	}
}

