package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.dto.booking.BookingSettingsResponse;
import com.dmdr.personal.portal.booking.dto.booking.UpdateBookingSettingsRequest;
import com.dmdr.personal.portal.booking.model.AvailabilityRule;
import com.dmdr.personal.portal.booking.model.BookingSettings;
import com.dmdr.personal.portal.core.model.TimezoneEntry;
import com.dmdr.personal.portal.booking.model.OverrideStatus;
import com.dmdr.personal.portal.booking.repository.AvailabilityOverrideRepository;
import com.dmdr.personal.portal.booking.repository.AvailabilityRuleRepository;
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
    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final AvailabilityOverrideRepository availabilityOverrideRepository;

    public BookingSettingsServiceImpl(
            BookingSettingsRepository repository,
            AvailabilityRuleRepository availabilityRuleRepository,
            AvailabilityOverrideRepository availabilityOverrideRepository) {
        this.repository = repository;
        this.availabilityRuleRepository = availabilityRuleRepository;
        this.availabilityOverrideRepository = availabilityOverrideRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BookingSettingsResponse getSettings() {
        BookingSettings settings = repository.mustFindTopByOrderByIdAsc();
        return toResponse(settings);
    }

    @Override
    @Transactional
    public BookingSettingsResponse updateSettings(UpdateBookingSettingsRequest request) {
        BookingSettings settings = repository.mustFindTopByOrderByIdAsc();

        settings.setBookingSlotsInterval(request.getBookingSlotsInterval());
        settings.setBookingFirstSlotInterval(request.getBookingFirstSlotInterval());
        settings.setBookingCancelationInterval(request.getBookingCancelationInterval());
        settings.setBookingUpdatingInterval(request.getBookingUpdatingInterval());

        if (!settings.getDefaultTimezoneId().equals(request.getDefaultTimezoneId())) {
            // Validate that there are no non-ARCHIVED rules or overrides before changing
            // timezone
            validateNoNonArchivedRulesOrOverrides();

            // Update timezone and calculate offset dynamically
            TimezoneEntry timezone = TimezoneEntry.getById(request.getDefaultTimezoneId());
            settings.setDefaultTimezoneId(request.getDefaultTimezoneId());
            settings.setDefaultUtcOffset(timezone.getGmtOffset());
        }
        BookingSettings saved = repository.save(settings);
        return toResponse(saved);
    }

    private static BookingSettingsResponse toResponse(BookingSettings settings) {
        BookingSettingsResponse resp = new BookingSettingsResponse();
        resp.setId(settings.getId());
        resp.setBookingSlotsInterval(settings.getBookingSlotsInterval());
        resp.setBookingFirstSlotInterval(settings.getBookingFirstSlotInterval());
        resp.setBookingCancelationInterval(settings.getBookingCancelationInterval());
        resp.setBookingUpdatingInterval(settings.getBookingUpdatingInterval());
        resp.setDefaultTimezone(TimezoneEntry.getById(settings.getDefaultTimezoneId()));
        resp.setDefaultUtcOffset(settings.getDefaultUtcOffset());
        return resp;
    }

    @Override
    @Transactional(readOnly = true)
    public TimezoneEntry getDefaultTimezone() {
        BookingSettings settings = repository.mustFindTopByOrderByIdAsc();

        Integer timezoneId = settings.getDefaultTimezoneId();
        if (timezoneId == null) {
            throw new IllegalStateException(
                    "BookingSettings defaultTimezone is not configured. Please set a default timezone in booking settings.");
        }

        return TimezoneEntry.getById(timezoneId);
    }

    private void validateNoNonArchivedRulesOrOverrides() {
        long nonArchivedRulesCount = availabilityRuleRepository.countNonArchivedRules(
                AvailabilityRule.RuleStatus.ARCHIVED);
        long nonArchivedOverridesCount = availabilityOverrideRepository.countNonArchivedOverrides(
                OverrideStatus.ARCHIVED);

        if (nonArchivedRulesCount > 0 || nonArchivedOverridesCount > 0) {
            StringBuilder errorMessage = new StringBuilder(
                    "Cannot change timezone: there are existing rules or overrides that are not ARCHIVED. ");

            if (nonArchivedRulesCount > 0) {
                errorMessage.append("Found ").append(nonArchivedRulesCount)
                        .append(" non-ARCHIVED rule(s). ");
            }

            if (nonArchivedOverridesCount > 0) {
                errorMessage.append("Found ").append(nonArchivedOverridesCount)
                        .append(" non-ARCHIVED override(s). ");
            }

            errorMessage.append("Please delete or archive all rules and overrides before changing the timezone.");

            throw new IllegalArgumentException(errorMessage.toString());
        }
    }
}
