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

@Controller
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;

    @GetMapping("/booking")
    public String bookingPage(Model model) {
        model.addAttribute("availableSlots", bookingService.getAvailableSlots());
        model.addAttribute("bookingRequest", new BookingRequestDto());
        return "booking";
    }

    @PostMapping("/booking/create")
    public String create(@AuthenticationPrincipal UserDetails principal,
                         @ModelAttribute BookingRequestDto dto) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        bookingService.createBooking(user, dto.getSlotId());
        return "redirect:/account/bookings";
    }
}
