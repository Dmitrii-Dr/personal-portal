package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.model.User;
import com.dmdr.personal.portal.service.BookingService;
import com.dmdr.personal.portal.service.PersonalContentService;
import com.dmdr.personal.portal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AccountController {

    private final BookingService bookingService;
    private final PersonalContentService contentService;
    private final UserService userService;

    @GetMapping("/account/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
        model.addAttribute("userEmail", principal.getUsername());
        return "account/dashboard";
    }

    @GetMapping("/account/bookings")
    public String myBookings(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userService.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        model.addAttribute("bookings", bookingService.getUserBookings(user));
        return "account/my_bookings";
    }

    @GetMapping("/account/content")
    public String content(Model model) {
        model.addAttribute("contentList", contentService.findAll());
        return "account/content";
    }
}
