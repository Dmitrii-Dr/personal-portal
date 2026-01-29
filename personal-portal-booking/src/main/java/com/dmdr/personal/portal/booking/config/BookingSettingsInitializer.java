package com.dmdr.personal.portal.booking.config;

import com.dmdr.personal.portal.booking.model.BookingSettings;
import com.dmdr.personal.portal.booking.repository.BookingSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initializes default BookingSettings on application startup if none exist.
 * This prevents IllegalStateException when the application tries to access
 * booking settings.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingSettingsInitializer implements ApplicationRunner {

    private final BookingSettingsRepository bookingSettingsRepository;

    // Default values for booking settings (all intervals are in minutes)
    private static final int DEFAULT_BOOKING_SLOTS_INTERVAL = 60; // 1 hour slots
    private static final int DEFAULT_BOOKING_FIRST_SLOT_INTERVAL = 120; // 2 hours minimum notice
    private static final int DEFAULT_BOOKING_CANCELATION_INTERVAL = 60; // 1 hour before session
    private static final int DEFAULT_BOOKING_UPDATING_INTERVAL = 60; // 1 hour before session
    private static final Integer DEFAULT_TIMEZONE_ID = 13; // UTC
    private static final String DEFAULT_UTC_OFFSET = "+00:00";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (bookingSettingsRepository.findTopByOrderByIdAsc().isEmpty()) {
            log.info("No BookingSettings found. Creating default BookingSettings...");

            BookingSettings defaultSettings = new BookingSettings();
            defaultSettings.setBookingSlotsInterval(DEFAULT_BOOKING_SLOTS_INTERVAL);
            defaultSettings.setBookingFirstSlotInterval(DEFAULT_BOOKING_FIRST_SLOT_INTERVAL);
            defaultSettings.setBookingCancelationInterval(DEFAULT_BOOKING_CANCELATION_INTERVAL);
            defaultSettings.setBookingUpdatingInterval(DEFAULT_BOOKING_UPDATING_INTERVAL);
            defaultSettings.setDefaultTimezoneId(DEFAULT_TIMEZONE_ID);
            defaultSettings.setDefaultUtcOffset(DEFAULT_UTC_OFFSET);

            bookingSettingsRepository.save(defaultSettings);

            log.info("Default BookingSettings created successfully with ID: {}", defaultSettings.getId());
            log.info(
                    "BookingSettings: slots={}min, firstSlot={}min, cancelation={}min, updating={}min, timezone={}, offset={}",
                    DEFAULT_BOOKING_SLOTS_INTERVAL,
                    DEFAULT_BOOKING_FIRST_SLOT_INTERVAL,
                    DEFAULT_BOOKING_CANCELATION_INTERVAL,
                    DEFAULT_BOOKING_UPDATING_INTERVAL,
                    DEFAULT_TIMEZONE_ID,
                    DEFAULT_UTC_OFFSET);
        } else {
            log.info("BookingSettings already exists. Skipping initialization.");
        }
    }
}
