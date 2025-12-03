package com.dmdr.personal.portal.controller.util;

import com.dmdr.personal.portal.booking.model.BookingStatus;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class BookingStatusParser {

	private BookingStatusParser() {
		// Utility class - prevent instantiation
	}

	public static Set<BookingStatus> parseStatuses(String statusParam) {
		if (statusParam == null || statusParam.trim().isEmpty()) {
			return Set.of();
		}
		try {
			return Arrays.stream(statusParam.split(","))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.map(BookingStatus::valueOf)
					.collect(Collectors.toSet());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid booking status. Valid values are: " + 
					Arrays.toString(BookingStatus.values()), e);
		}
	}
}

