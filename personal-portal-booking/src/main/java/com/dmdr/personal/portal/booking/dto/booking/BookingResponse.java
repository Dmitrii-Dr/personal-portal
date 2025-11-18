package com.dmdr.personal.portal.booking.dto.booking;

import com.dmdr.personal.portal.booking.model.BookingStatus;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingResponse {
	private Long id;
	private Long sessionTypeId;
	private String sessionTypeName;
	private Instant startTime;
	private Instant endTime;
	private BookingStatus status;
	private String clientMessage;
	private Instant createdAt;
}

