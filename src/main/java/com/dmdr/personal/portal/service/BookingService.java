package com.dmdr.personal.portal.service;

import com.dmdr.personal.portal.model.Booking;
import com.dmdr.personal.portal.model.User;

import java.util.List;

public interface BookingService {
    List<?> getAvailableSlots();
    Booking createBooking(User client, Long slotId);
    List<Booking> getUserBookings(User client);
}
