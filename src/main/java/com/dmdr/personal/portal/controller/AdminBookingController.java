package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.model.BookingSlot;
import com.dmdr.personal.portal.repository.BookingSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    private final BookingSlotRepository slotRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("slots", slotRepository.findAll());
        return "admin/slots";
    }

    @PostMapping("/create")
    public String create(@RequestParam Instant startTime, @RequestParam Instant endTime) {
        BookingSlot slot = BookingSlot.builder()
                .startTime(startTime)
                .endTime(endTime)
                .booked(false)
                .build();
        slotRepository.save(slot);
        return "redirect:/admin/bookings";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        slotRepository.deleteById(id);
        return "redirect:/admin/bookings";
    }
}
