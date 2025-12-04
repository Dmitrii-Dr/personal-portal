package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.booking.dto.booking.AdminBookingResponse;
import com.dmdr.personal.portal.booking.dto.booking.BookingResponse;
import com.dmdr.personal.portal.booking.dto.booking.AdminBookingsGroupedByStatusResponse;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingStatusRequest;
import com.dmdr.personal.portal.booking.model.Booking;
import com.dmdr.personal.portal.booking.model.BookingStatus;
import com.dmdr.personal.portal.booking.service.BookingService;
import com.dmdr.personal.portal.controller.util.BookingStatusParser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/session/booking")
public class AdminBookingController {

    private final BookingService bookingService;

    public AdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<Page<AdminBookingResponse>> getBookingsByStatus(
            @PathVariable("status") BookingStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Booking> bookingsPage = bookingService.getAllBookingsByStatus(status, pageable);
        Page<AdminBookingResponse> adminBookingsPage = bookingsPage.map(booking -> {
            BookingResponse bookingResponse = toBookingResponse(booking);
            return new AdminBookingResponse(bookingResponse, booking.getClient());
        });
        return ResponseEntity.ok(adminBookingsPage);
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

    @GetMapping("/group")
    public ResponseEntity<AdminBookingsGroupedByStatusResponse> getBookingsByStatuses(
            @RequestParam(value = "status", required = false) String statusParam) {
        Set<BookingStatus> statuses = BookingStatusParser.parseStatuses(statusParam);
        AdminBookingsGroupedByStatusResponse response = bookingService.getBookingsGroupedByStatus(statuses);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/status")
    public ResponseEntity<BookingResponse> updateBookingStatus(@Valid @RequestBody UpdateBookingStatusRequest request) {
        BookingResponse updatedBooking = bookingService.updateStatus(request);
        return ResponseEntity.ok(updatedBooking);
    }
}
