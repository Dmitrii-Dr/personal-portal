package com.dmdr.personal.portal.service;

import com.dmdr.personal.portal.model.Booking;
import com.dmdr.personal.portal.model.User;

import java.util.List;

import com.dmdr.personal.portal.model.BookingSlot;
import java.time.LocalDate;

public interface BookingService {
    List<BookingSlot> getAvailableSlots();
    List<BookingSlot> getAvailableSlotsForDate(LocalDate date);
    Booking createBooking(User client, Long slotId);
    List<Booking> getUserBookings(User client);
}
