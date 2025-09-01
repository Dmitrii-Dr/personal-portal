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

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingSlotRepository slotRepository;
    private final BookingRepository bookingRepository;
    private final EmailService emailService;

    @Override
    public List<BookingSlot> getAvailableSlots() {
        return slotRepository.findByBookedFalseOrderByStartTimeAsc();
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
