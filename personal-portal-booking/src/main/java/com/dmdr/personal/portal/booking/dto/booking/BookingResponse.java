package com.dmdr.personal.portal.booking.dto.booking;

import com.dmdr.personal.portal.booking.model.BookingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingResponse {
	private Long id;
	private String sessionName;
	private Integer sessionDurationMinutes;
	private Integer sessionBufferMinutes;
	private Map<String, BigDecimal> sessionPrices;
	private String sessionDescription;
	private Instant startTimeInstant;
	private Instant endTimeInstant;
	private BookingStatus status;
	private String clientMessage;
	private Instant createdAt;
}

