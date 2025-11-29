package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.booking.dto.booking.AdminBookingResponse;
import com.dmdr.personal.portal.booking.dto.booking.BookingResponse;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingStatusRequest;
import com.dmdr.personal.portal.booking.model.Booking;
import com.dmdr.personal.portal.booking.model.BookingStatus;
import com.dmdr.personal.portal.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/bookings")
public class AdminBookingController {

    private final BookingService bookingService;

    public AdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<AdminBookingResponse>> getBookingsByStatus(@PathVariable("status") BookingStatus status) {
        List<Booking> bookings = bookingService.getAllBookingsByStatus(status);
        List<AdminBookingResponse> adminBookings = bookings.stream()
                .map(booking -> {
                    BookingResponse bookingResponse = toBookingResponse(booking);
                    return new AdminBookingResponse(bookingResponse, booking.getClient());
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(adminBookings);
    }

    private BookingResponse toBookingResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setSessionTypeId(booking.getSessionType().getId());
        response.setSessionTypeName(booking.getSessionType().getName());
        response.setStartTimeInstant(booking.getStartTime());
        response.setEndTimeInstant(booking.getEndTime());
        response.setStatus(booking.getStatus());
        response.setClientMessage(booking.getClientMessage());
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }

    @PutMapping("/status")
    public ResponseEntity<BookingResponse> updateBookingStatus(@Valid @RequestBody UpdateBookingStatusRequest request) {
        BookingResponse updatedBooking = bookingService.updateStatus(request);
        return ResponseEntity.ok(updatedBooking);
    }
}
