package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.booking.BookingSettingsResponse;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingSettingsRequest;
import com.dmdr.personal.portal.booking.model.BookingSettings;
import com.dmdr.personal.portal.booking.repository.BookingSettingsRepository;
import com.dmdr.personal.portal.booking.service.BookingSettingsService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingSettingsServiceImpl implements BookingSettingsService {

    private final BookingSettingsRepository repository;

    public BookingSettingsServiceImpl(BookingSettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingSettingsResponse getSettings() {
        BookingSettings settings = repository.findTopByOrderByIdAsc()
                .orElseGet(() -> {
                    BookingSettings s = new BookingSettings();
                    s.setBookingSlotsInterval(15);
                    s.setBookingCancelationInterval(0);
                    s.setBookingUpdatingInterval(0);
                    s.setDefaultTimezone("UTC");
                    s.setDefaultUtcOffset("+00:00");
                    return s;
                });
        return toResponse(settings);
    }

    @Override
    @Transactional
    public BookingSettingsResponse updateSettings(UpdateBookingSettingsRequest request) {
        // TODO Create default settings when deploy application
        BookingSettings settings = repository.findTopByOrderByIdAsc().orElseGet(() -> {
            BookingSettings s = new BookingSettings();
            s.setDefaultTimezone("UTC");
            s.setDefaultUtcOffset("+00:00");
            return s;
        });
        
        settings.setBookingSlotsInterval(request.getBookingSlotsInterval());
        settings.setBookingCancelationInterval(request.getBookingCancelationInterval());
        settings.setBookingUpdatingInterval(request.getBookingUpdatingInterval());

        if(settings.getDefaultTimezone() != request.getDefaultTimezone()) {
            // Update timezone and calculate offset dynamically
            String calculatedOffset = calculateOffsetFromTimezone(request.getDefaultTimezone());
            settings.setDefaultTimezone(request.getDefaultTimezone());
            settings.setDefaultUtcOffset(calculatedOffset);
        }
        
        BookingSettings saved = repository.save(settings);
        return toResponse(saved);
    }

    private static BookingSettingsResponse toResponse(BookingSettings settings) {
        BookingSettingsResponse resp = new BookingSettingsResponse();
        resp.setId(settings.getId());
        resp.setBookingSlotsInterval(settings.getBookingSlotsInterval());
        resp.setBookingCancelationInterval(settings.getBookingCancelationInterval());
        resp.setBookingUpdatingInterval(settings.getBookingUpdatingInterval());
        resp.setDefaultTimezone(settings.getDefaultTimezone());
        resp.setDefaultUtcOffset(settings.getDefaultUtcOffset());
        return resp;
    }

    private static String calculateOffsetFromTimezone(String timezone) {
        Instant referenceTime = Instant.now();
        try {
            ZoneId zoneId = ZoneId.of(timezone);
            ZoneOffset offset = zoneId.getRules().getOffset(referenceTime);
            return offset.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timezone: " + e.getMessage(), e);
        }
    }
}
