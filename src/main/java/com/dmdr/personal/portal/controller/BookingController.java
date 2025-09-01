package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.dto.BookingRequestDto;
import com.dmdr.personal.portal.model.User;
import com.dmdr.personal.portal.service.BookingService;
import com.dmdr.personal.portal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;

    @GetMapping("/booking")
    public String bookingPage(@RequestParam(value = "date", required = false) String dateStr, Model model) {
        LocalDate today = LocalDate.now();
        LocalDate selectedDate = today;
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                selectedDate = LocalDate.parse(dateStr);
            } catch (Exception ignored) {}
        }
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("availableSlots", bookingService.getAvailableSlotsForDate(selectedDate));
        model.addAttribute("bookingRequest", new BookingRequestDto());
        // For calendar: pass min/max selectable dates
        model.addAttribute("minDate", today);
        model.addAttribute("maxDate", today.plusDays(7));
        return "booking";
    }

    @PostMapping("/booking/create")
    public String create(@AuthenticationPrincipal UserDetails principal,
                         @ModelAttribute BookingRequestDto dto) {
        if (dto.getSlotId() == null) {
            return "redirect:/booking?error=noslot";
        }
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        bookingService.createBooking(user, dto.getSlotId());
        return "redirect:/account/bookings";
    }
}
