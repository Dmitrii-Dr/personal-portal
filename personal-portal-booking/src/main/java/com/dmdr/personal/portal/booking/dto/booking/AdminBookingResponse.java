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

	public AdminBookingResponse(BookingResponse bookingResponse, User client) {
		// Copy all fields from BookingResponse
		this.setId(bookingResponse.getId());
		this.setSessionTypeId(bookingResponse.getSessionTypeId());
		this.setSessionTypeName(bookingResponse.getSessionTypeName());
		this.setStartTimeInstant(bookingResponse.getStartTimeInstant());
		this.setEndTimeInstant(bookingResponse.getEndTimeInstant());
		this.setStatus(bookingResponse.getStatus());
		this.setClientMessage(bookingResponse.getClientMessage());
		this.setCreatedAt(bookingResponse.getCreatedAt());
		
		// Add client information
		this.clientId = client.getId();
		this.clientEmail = client.getEmail();
		this.clientFirstName = client.getFirstName();
		this.clientLastName = client.getLastName();
	}
}

