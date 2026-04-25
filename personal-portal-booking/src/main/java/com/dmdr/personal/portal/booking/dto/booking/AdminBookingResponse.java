package com.dmdr.personal.portal.booking.dto.booking;

import com.dmdr.personal.portal.users.model.User;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AdminBookingResponse extends BookingResponse {
	private UUID clientId;
	private String clientEmail;
	private String clientFirstName;
	private String clientLastName;
	private String clientPhoneNumber;

	public AdminBookingResponse(BookingResponse bookingResponse, User client) {
		this(
				bookingResponse,
				client != null ? client.getId() : null,
				client != null ? client.getEmail() : null,
				client != null ? client.getFirstName() : null,
				client != null ? client.getLastName() : null,
				client != null ? client.getPhoneNumber() : null);
	}

	public AdminBookingResponse(BookingResponse bookingResponse, UUID clientId, String clientEmail,
			String clientFirstName, String clientLastName, String clientPhoneNumber) {
		// Copy all fields from BookingResponse
		this.setId(bookingResponse.getId());
		this.setSessionName(bookingResponse.getSessionName());
		this.setSessionDurationMinutes(bookingResponse.getSessionDurationMinutes());
		this.setSessionBufferMinutes(bookingResponse.getSessionBufferMinutes());
		this.setSessionPrices(bookingResponse.getSessionPrices());
		this.setSessionDescription(bookingResponse.getSessionDescription());
		this.setStartTimeInstant(bookingResponse.getStartTimeInstant());
		this.setEndTimeInstant(bookingResponse.getEndTimeInstant());
		this.setStatus(bookingResponse.getStatus());
		this.setClientMessage(bookingResponse.getClientMessage());
		this.setCreatedAt(bookingResponse.getCreatedAt());
		
		// Add client information
		this.clientId = clientId;
		this.clientEmail = clientEmail;
		this.clientFirstName = clientFirstName;
		this.clientLastName = clientLastName;
		this.clientPhoneNumber = clientPhoneNumber;
	}
}

