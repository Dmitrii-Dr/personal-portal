package com.dmdr.personal.portal.config;

import com.dmdr.personal.portal.model.BookingSlot;
import com.dmdr.personal.portal.repository.BookingSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Initializes default booking slots (hourly 10:00-18:00) for the current day
 * if none exist yet for today. Prevents empty booking UI after a fresh deploy.
 */
@Component
@RequiredArgsConstructor
public class BookingSlotInitializer implements CommandLineRunner {

    private final BookingSlotRepository slotRepository;

    @Override
    public void run(String... args) {
        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();
        Instant dayStart = today.atStartOfDay(zone).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant();

        boolean existsToday = slotRepository.existsByStartTimeBetween(dayStart, dayEnd);
        if (!existsToday) {
            generateForDate(today, zone);
        }
    }

    private void generateForDate(LocalDate date, ZoneId zone) {
    // Generate hourly slots 12:00–18:00 (last slot: 17:00–18:00)
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
}
