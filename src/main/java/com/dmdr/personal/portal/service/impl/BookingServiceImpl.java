package com.dmdr.personal.portal.service.impl;

import com.dmdr.personal.portal.model.Booking;
import com.dmdr.personal.portal.model.BookingSlot;
import com.dmdr.personal.portal.model.User;
import com.dmdr.personal.portal.repository.BookingRepository;
import com.dmdr.personal.portal.repository.BookingSlotRepository;
import com.dmdr.personal.portal.service.BookingService;
import com.dmdr.personal.portal.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingSlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final EmailService emailService;

    @Override
    public List<BookingSlot> getAvailableSlots() {
        return getAvailableSlotsForDate(LocalDate.now());
    }

    @Override
    public List<BookingSlot> getAvailableSlotsForDate(LocalDate date) {
        ZoneId zone = ZoneId.systemDefault();
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
        // If no slots exist for this date, create standard slots from 12:00 to 18:00
        boolean exists = slotRepository.existsByStartTimeBetween(dayStart, dayEnd);
        if (!exists) {
            for (int hour = 12; hour < 18; hour++) {
                Instant start = date.atTime(hour, 0).atZone(zone).toInstant();
                Instant end = date.atTime(hour + 1, 0).atZone(zone).toInstant();
                BookingSlot slot = BookingSlot.builder()
                        .startTime(start)
                        .endTime(end)
                        .booked(false)
                        .build();
                slotRepository.save(slot);
            }
        }

        return slotRepository.findByBookedFalseAndStartTimeBetweenOrderByStartTimeAsc(dayStart, dayEnd);
    }

    @Override
    @Transactional
    public Booking createBooking(User client, Long slotId) {
        BookingSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        if (slot.isBooked()) {
            throw new IllegalStateException("Slot already booked");
        }
    slot.setBooked(true);
    // Persist the slot state immediately so available slot queries reflect the change
    slotRepository.save(slot);
        Booking booking = Booking.builder()
                .client(client)
                .slot(slot)
                .status("CONFIRMED") // TODO: Handle workflow statuses
                .createdAt(Instant.now())
                .build();
        booking = bookingRepository.save(booking);
        emailService.sendBookingConfirmation(booking);
        return booking;
    }

    @Override
    public List<Booking> getUserBookings(User client) {
        return bookingRepository.findByClientOrderByCreatedAtDesc(client);
    }
}
