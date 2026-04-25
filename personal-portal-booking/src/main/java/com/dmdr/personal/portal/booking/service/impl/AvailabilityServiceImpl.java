package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.core.model.TimezoneEntry;
import com.dmdr.personal.portal.booking.dto.booking.BookingSuggestion;
import com.dmdr.personal.portal.booking.model.AvailabilityOverride;
import com.dmdr.personal.portal.booking.model.AvailabilityRule;
import com.dmdr.personal.portal.booking.model.Booking;
import com.dmdr.personal.portal.booking.model.BookingSettings;
import com.dmdr.personal.portal.booking.model.BookingStatus;
import com.dmdr.personal.portal.booking.model.OverrideStatus;
import com.dmdr.personal.portal.booking.model.SessionType;
import com.dmdr.personal.portal.booking.repository.AvailabilityOverrideRepository;
import com.dmdr.personal.portal.booking.repository.AvailabilityRuleRepository;
import com.dmdr.personal.portal.booking.repository.BookingRepository;
import com.dmdr.personal.portal.booking.repository.BookingSettingsRepository;
import com.dmdr.personal.portal.booking.service.AvailabilityService;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityServiceImpl implements AvailabilityService {

    private static final int MAX_LOOKAHEAD_DAYS = 365;

    private final AvailabilityRuleRepository repository;
    private final BookingSettingsRepository bookingSettingsRepository;
    private final AvailabilityOverrideRepository overrideRepository;
    private final BookingRepository bookingRepository;

    public AvailabilityServiceImpl(
            AvailabilityRuleRepository repository,
            BookingSettingsRepository bookingSettingsRepository,
            AvailabilityOverrideRepository overrideRepository,
            BookingRepository bookingRepository) {
        this.repository = repository;
        this.bookingSettingsRepository = bookingSettingsRepository;
        this.overrideRepository = overrideRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void validateBookingAvailability(Instant requestedStartTime, Instant requestedEndTime) {
        validateBookingAvailabilityInternal(requestedStartTime, requestedEndTime, null);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateBookingAvailabilityForUpdate(Booking updatingBooking, Instant requestedStartTime, Instant requestedEndTime) {
        validateBookingAvailabilityInternal(requestedStartTime, requestedEndTime, updatingBooking);
    }

    private void validateBookingAvailabilityInternal(
            Instant requestedStartTime,
            Instant requestedEndTime,
            Booking updatingBooking) {
        if (requestedStartTime.isAfter(requestedEndTime) || requestedStartTime.equals(requestedEndTime)) {
            throw new IllegalArgumentException("Requested start time must be before end time");
        }

        // Get booking settings first to get the default timezone
        BookingSettings settings = bookingSettingsRepository.mustFindTopByOrderByIdAsc();
        ZoneId zoneId = ZoneId.of(TimezoneEntry.getById(settings.getDefaultTimezoneId()).getGmtOffset());
        LocalDate requestedDate = requestedStartTime.atZone(zoneId).toLocalDate();

        // Calculate day boundaries in the default timezone
        LocalDateTime startOfDay = requestedDate.atStartOfDay();
        LocalDateTime endOfDay = requestedDate.atTime(23, 59, 59, 999_999_999);
        Instant dayStartInstant = startOfDay.atZone(zoneId).toInstant();
        Instant dayEndInstant = endOfDay.atZone(zoneId).toInstant();

        // The case when schedule is empty and the first slot is shown based on BookingFirstSlotInterval
        // If user select the first next available slot (today, or another day depending on BookingFirstSlotInterval)
        // Some time past while user submitting the request and validation of minimumStartTime fails
        // Here we relax minimumStartTime validation and allow 10 minutes delay if it's around defaultMinimumStartTime
        Instant now = Instant.now();
        Instant defaultMinimumStartTime = now.plusSeconds(settings.getBookingFirstSlotInterval() * 60L);

        Instant minimumStartTime = defaultMinimumStartTime;
        if (requestedStartTime.isAfter(now) && requestedStartTime.isBefore(defaultMinimumStartTime)) {
            long secondsBeforeDefault = Duration.between(requestedStartTime, defaultMinimumStartTime).getSeconds();
            //10 minutes relaxation for MinimumStartTime validation
            if (secondsBeforeDefault > 10 * 60L) {
                throw new IllegalArgumentException(
                        "Requested start time is too early. " +
                                "Start: " + requestedStartTime +
                                ", Minimum allowed: " + defaultMinimumStartTime +
                                ", Please try to select a new slot");
            }
            minimumStartTime = requestedStartTime;
        }

        // Follow the same logic as calculateBookingSuggestion:
        // 1. Find matching rules (may be empty, but availability can come from
        // overrides)
        // 2. Calculate available time ranges (which includes subtracting unavailable,
        // adding available overrides, and subtracting booked)
        List<TimeRange> allAvailableRanges = updatingBooking == null
                ? calculateAvailableTimeRanges(
                dayStartInstant, dayEndInstant, requestedDate, zoneId, minimumStartTime)
                : calculateAvailableTimeRangesForUpdate(
                dayStartInstant, dayEndInstant, requestedDate, zoneId, minimumStartTime, updatingBooking);

        // Check if requested time range is fully contained in any available range
        TimeRange requestedRange = new TimeRange(requestedStartTime, requestedEndTime);
        boolean isAvailable = allAvailableRanges.stream()
                .anyMatch(availableRange -> {
                    TimeRange overlap = findOverlap(availableRange, requestedRange);
                    // The overlap must exactly match the requested range (fully contained)
                    return overlap != null &&
                            overlap.getStart().equals(requestedStartTime) &&
                            overlap.getEnd().equals(requestedEndTime);
                });

        if (!isAvailable) {
            throw new IllegalArgumentException(
                    "Requested time range is not available for booking. " +
                            "Start: " + requestedStartTime + ", End: " + requestedEndTime);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateBookingAvailabilityForAdmin(Instant requestedStartTime, Instant requestedEndTime) {
        if (requestedStartTime.isAfter(requestedEndTime) || requestedStartTime.equals(requestedEndTime)) {
            throw new IllegalArgumentException("Requested start time must be before end time");
        }

        // Check for overlapping bookings with CONFIRMED or PENDING_APPROVAL status
        // This method does NOT check availability rules or overrides - it assumes
        // admins can create bookings even when no availability rules exist for that
        // time
        List<Booking> overlappingBookings = bookingRepository.findBookingsByStatusAndTimeRange(
                List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING_APPROVAL),
                requestedStartTime,
                requestedEndTime);

        if (!overlappingBookings.isEmpty()) {
            throw new IllegalArgumentException(
                    "Requested time range overlaps with existing bookings. " +
                            "Start: " + requestedStartTime + ", End: " + requestedEndTime);
        }
    }

    public void validateBookingAvailabilityForAdminForUpdate(Booking updatingBooking, Instant requestedStartTime, Instant requestedEndTime) {
        if (requestedStartTime.isAfter(requestedEndTime) || requestedStartTime.equals(requestedEndTime)) {
            throw new IllegalArgumentException("Requested start time must be before end time");
        }

        // Check for overlapping bookings with CONFIRMED or PENDING_APPROVAL status
        // This method does NOT check availability rules or overrides - it assumes
        // admins can create bookings even when no availability rules exist for that
        // time
        List<Booking> overlappingBookings = bookingRepository.findBookingsByStatusAndTimeRange(
                List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING_APPROVAL),
                requestedStartTime,
                requestedEndTime);

        if (overlappingBookings.isEmpty()) {
            return;
        }
        if (overlappingBookings.size() > 1 || !overlappingBookings.getFirst().getId().equals(updatingBooking.getId()))
            throw new IllegalArgumentException(
                    "Requested time range overlaps with existing bookings: " + overlappingBookings);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSuggestion> calculateBookingSuggestion(
            SessionType sessionType,
            LocalDate suggestedDate,
            Integer timezoneId) {
        SuggestionContext context = buildSuggestionContext(suggestedDate, timezoneId);

        // Calculate available time ranges
        List<TimeRange> allAvailableRanges = calculateAvailableTimeRanges(
                context.dayStartInstant,
                context.dayEndInstant,
                suggestedDate,
                context.zoneId,
                context.minimumStartTime);

        List<BookingSuggestion> suggestions = new ArrayList<>();
        for (TimeRange availableRange : allAvailableRanges) {
            suggestions
                    .addAll(generateSlots(
                                availableRange,
                                sessionType.getDurationMinutes(),
                                sessionType.getBufferMinutes(),
                                context));
        }

        return suggestions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSuggestion> calculateBookingSuggestionForUpdate(
            Booking bookingToUpdate,
            LocalDate suggestedDate,
            Integer timezoneId) {

        SuggestionContext context = buildSuggestionContext(suggestedDate, timezoneId);

        // Calculate available time ranges
        List<TimeRange> allAvailableRanges = calculateAvailableTimeRangesForUpdate(
                context.dayStartInstant,
                context.dayEndInstant,
                suggestedDate,
                context.zoneId,
                context.minimumStartTime,
                bookingToUpdate);

        List<BookingSuggestion> suggestions = new ArrayList<>();
        for (TimeRange availableRange : allAvailableRanges) {
            List<BookingSuggestion> slots = generateSlotsForUpdate(
                    availableRange,
                    bookingToUpdate,
                    context);

            suggestions.addAll(slots);
        }

        return suggestions;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDate> calculateAvailableDays(SessionType sessionType, Integer timezoneId) {
        return calculateAvailableDaysInternal(
                sessionType.getDurationMinutes(),
                sessionType.getBufferMinutes(),
                timezoneId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDate> calculateAvailableDaysForUpdate(Booking booking, Integer timezoneId) {
        return calculateAvailableDaysInternal(
                booking.getSessionDurationMinutes(),
                booking.getSessionBufferMinutes(),
                timezoneId);
    }

    private List<LocalDate> calculateAvailableDaysInternal(
            int sessionDurationMinutes,
            int sessionBufferMinutes,
            Integer timezoneId) {
        ZoneId userZone = ZoneId.of(TimezoneEntry.getById(timezoneId).getGmtOffset());
        LocalDate today = LocalDate.now(userZone);
        LocalDate capDate = today.plusDays(MAX_LOOKAHEAD_DAYS);

        BookingSettings settings = bookingSettingsRepository.mustFindTopByOrderByIdAsc();
        Instant minimumStart = Instant.now().plusSeconds(settings.getBookingFirstSlotInterval() * 60L);

        Instant todayStart = today.atStartOfDay(userZone).toInstant();
        Instant capInstant = capDate.atTime(23, 59, 59, 999_999_999).atZone(userZone).toInstant();

        List<AvailabilityRule> allActiveRules = repository.findByRuleStatus(AvailabilityRule.RuleStatus.ACTIVE);
        List<AvailabilityOverride> activeAvailableOverrides =
                overrideRepository.findOverlappingAvailableOverrides(todayStart, capInstant, OverrideStatus.ACTIVE);

        if (allActiveRules.isEmpty() && activeAvailableOverrides.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDate maxDateFromRules = allActiveRules.stream()
                .map(r -> r.getRuleEndInstant().atZone(userZone).toLocalDate())
                .max(Comparator.naturalOrder())
                .orElse(today);

        LocalDate maxDateFromOverrides = activeAvailableOverrides.stream()
                .map(o -> o.getOverrideEndInstant().atZone(userZone).toLocalDate())
                .max(Comparator.naturalOrder())
                .orElse(today);

        LocalDate maxDate = maxDateFromRules.isAfter(maxDateFromOverrides) ? maxDateFromRules : maxDateFromOverrides;
        if (maxDate.isAfter(capDate)) {
            maxDate = capDate;
        }

        List<LocalDate> result = new ArrayList<>();
        for (LocalDate date = today; !date.isAfter(maxDate); date = date.plusDays(1)) {
            Instant dayStart = date.atStartOfDay(userZone).toInstant();
            Instant dayEnd = date.atTime(23, 59, 59, 999_999_999).atZone(userZone).toInstant();

            List<TimeRange> ranges = calculateAvailableTimeRangesWithPreloadedRules(
                    allActiveRules, dayStart, dayEnd, date, userZone, minimumStart);

            SuggestionContext ctx = new SuggestionContext(userZone, dayStart, dayEnd, settings, minimumStart);
            boolean hasSlot = ranges.stream()
                    .anyMatch(range -> !generateSlots(
                            range, sessionDurationMinutes, sessionBufferMinutes, ctx).isEmpty());

            if (hasSlot) {
                result.add(date);
            }
        }

        return result;
    }

    /**
     * Like calculateAvailableTimeRanges but accepts pre-loaded active rules to avoid
     * a repeated DB query per day when checking availability across many dates.
     */
    private List<TimeRange> calculateAvailableTimeRangesWithPreloadedRules(
            List<AvailabilityRule> allActiveRules,
            Instant dayStartInstant,
            Instant dayEndInstant,
            LocalDate date,
            ZoneId zoneId,
            Instant minimumStartTime) {

        List<TimeRange> allAvailableRanges = calculateAvailableTimeRangesByRulesAndOverridesWithPreloadedRules(
                allActiveRules, dayStartInstant, dayEndInstant, date, zoneId, minimumStartTime);

        List<Booking> existingBookings = bookingRepository.findBookingsByStatusAndTimeRange(
                List.of(BookingStatus.PENDING_APPROVAL, BookingStatus.CONFIRMED),
                dayStartInstant,
                dayEndInstant);

        return subtractBookedRangesFromAll(allAvailableRanges, existingBookings);
    }

    /**
     * Like calculateAvailableTimeRangesByRulesAndOverrides but filters rules from the
     * supplied pre-loaded list instead of issuing a per-day DB query.
     */
    private List<TimeRange> calculateAvailableTimeRangesByRulesAndOverridesWithPreloadedRules(
            List<AvailabilityRule> allActiveRules,
            Instant dayStartInstant,
            Instant dayEndInstant,
            LocalDate suggestedDate,
            ZoneId zoneId,
            Instant minimumStartTime) {

        List<AvailabilityRule> matchingRules = findMatchingRulesForDayFromList(
                allActiveRules, dayStartInstant, dayEndInstant, zoneId);

        List<AvailabilityOverride> unavailableOverrides = overrideRepository.findOverlappingUnavailableOverrides(
                dayStartInstant, dayEndInstant, OverrideStatus.ACTIVE);

        List<TimeRange> ruleRanges = new ArrayList<>();
        for (AvailabilityRule rule : matchingRules) {
            ZoneId ruleZoneId = ZoneId.of(TimezoneEntry.getById(rule.getTimezoneId()).getGmtOffset());
            LocalTime availableStart = rule.getAvailableStartTime();
            LocalTime availableEnd = rule.getAvailableEndTime();

            LocalDate startDateInRuleTimezone = dayStartInstant.atZone(ruleZoneId).toLocalDate();
            LocalDate endDateInRuleTimezone = dayEndInstant.atZone(ruleZoneId).toLocalDate();

            LocalDate candidateDate = startDateInRuleTimezone;
            while (!candidateDate.isAfter(endDateInRuleTimezone)) {
                DayOfWeek dayOfWeek = candidateDate.getDayOfWeek();
                if (containsDay(rule.getDaysOfWeekAsInt(), dayOfWeek.getValue())) {
                    LocalDateTime candidateStartDateTime = candidateDate.atTime(availableStart);
                    LocalDateTime candidateEndDateTime = candidateDate.atTime(availableEnd);
                    Instant candidateStartInstant = candidateStartDateTime.atZone(ruleZoneId).toInstant();
                    Instant candidateEndInstant = candidateEndDateTime.atZone(ruleZoneId).toInstant();

                    if (candidateStartInstant.isAfter(rule.getRuleEndInstant())) {
                        break;
                    }

                    TimeRange ruleOverlap = findFutureOverlap(
                            new TimeRange(dayStartInstant, dayEndInstant),
                            new TimeRange(candidateStartInstant, candidateEndInstant),
                            minimumStartTime);

                    if (ruleOverlap != null) {
                        ruleRanges.add(ruleOverlap);
                    }
                }
                candidateDate = candidateDate.plusDays(1);
            }
        }

        List<TimeRange> allAvailableRanges = subtractUnavailableRangesFromAll(ruleRanges, unavailableOverrides);

        List<AvailabilityOverride> availableOverrides = overrideRepository.findOverlappingAvailableOverrides(
                dayStartInstant, dayEndInstant, OverrideStatus.ACTIVE);

        for (AvailabilityOverride override : availableOverrides) {
            TimeRange overrideOverlap = findFutureOverlap(
                    new TimeRange(dayStartInstant, dayEndInstant),
                    new TimeRange(override.getOverideStartInstant(), override.getOverrideEndInstant()),
                    minimumStartTime);

            if (overrideOverlap != null) {
                allAvailableRanges.add(overrideOverlap);
            }
        }

        return allAvailableRanges;
    }

    /**
     * Like findMatchingRulesForDay but filters from a pre-loaded list instead of
     * querying the DB, so rules only need to be fetched once per multi-day iteration.
     */
    private List<AvailabilityRule> findMatchingRulesForDayFromList(
            List<AvailabilityRule> allActiveRules,
            Instant dayStartInstant,
            Instant dayEndInstant,
            ZoneId zoneId) {

        return allActiveRules.stream()
                .filter(rule -> {
                    Instant ruleStart = rule.getRuleStartInstant();
                    Instant ruleEnd = rule.getRuleEndInstant();

                    boolean ruleOverlapsDay = (ruleStart.isBefore(dayEndInstant) || ruleStart.equals(dayEndInstant))
                            && (ruleEnd.isAfter(dayStartInstant) || ruleEnd.equals(dayStartInstant));

                    if (!ruleOverlapsDay) {
                        return false;
                    }

                    ZoneId ruleZoneId = ZoneId.of(TimezoneEntry.getById(rule.getTimezoneId()).getGmtOffset());
                    LocalDate startDateInRuleTimezone = dayStartInstant.atZone(ruleZoneId).toLocalDate();
                    LocalDate endDateInRuleTimezone = dayEndInstant.atZone(ruleZoneId).toLocalDate();

                    DayOfWeek startDayOfWeek = startDateInRuleTimezone.getDayOfWeek();
                    DayOfWeek endDayOfWeek = endDateInRuleTimezone.getDayOfWeek();

                    boolean startDayMatches = containsDay(rule.getDaysOfWeekAsInt(), startDayOfWeek.getValue());
                    boolean endDayMatches = containsDay(rule.getDaysOfWeekAsInt(), endDayOfWeek.getValue());

                    return startDayMatches || endDayMatches;
                })
                .collect(Collectors.toList());
    }

    private SuggestionContext buildSuggestionContext(LocalDate suggestedDate, Integer timezoneId) {
        String timezone = TimezoneEntry.getById(timezoneId).getGmtOffset();
        ZoneId zoneId = ZoneId.of(timezone);

        // Convert suggestedDate to start and end of day in the given timezone
        LocalDateTime startOfDay = suggestedDate.atStartOfDay();
        LocalDateTime endOfDay = suggestedDate.atTime(23, 59, 59, 999_999_999);

        Instant dayStartInstant = startOfDay.atZone(zoneId).toInstant();
        Instant dayEndInstant = endOfDay.atZone(zoneId).toInstant();

        // Validate that dayStartInstant is not before the start of the current day
        LocalDate today = LocalDate.now(zoneId);
        Instant todayStartInstant = today.atStartOfDay().atZone(zoneId).toInstant();
        if (dayStartInstant.isBefore(todayStartInstant)) {
            throw new IllegalArgumentException(
                    "Cannot calculate booking suggestions for a date in the past. " +
                            "Suggested date: " + suggestedDate + " (" + timezone + "), " +
                            "Day start time: " + dayStartInstant.atZone(zoneId) + ", " +
                            "Today start time: " + todayStartInstant.atZone(zoneId));
        }

        // Get booking settings for slot interval
        BookingSettings settings = bookingSettingsRepository.mustFindTopByOrderByIdAsc();

        // Calculate minimum start time (now + bookingFirstSlotInterval)
        // This ensures sessions cannot be booked too soon (e.g., not within 5 minutes,
        // or not within 2 days)
        Instant minimumStartTime = Instant.now().plusSeconds(settings.getBookingFirstSlotInterval() * 60L);

        return new SuggestionContext(
                zoneId,
                dayStartInstant,
                dayEndInstant,
                settings,
                minimumStartTime);
    }

    private static class SuggestionContext {
        private final ZoneId zoneId;
        private final Instant dayStartInstant;
        private final Instant dayEndInstant;
        private final BookingSettings settings;
        private final Instant minimumStartTime;

        private SuggestionContext(
                ZoneId zoneId,
                Instant dayStartInstant,
                Instant dayEndInstant,
                BookingSettings settings,
                Instant minimumStartTime) {
            this.zoneId = zoneId;
            this.dayStartInstant = dayStartInstant;
            this.dayEndInstant = dayEndInstant;
            this.settings = settings;
            this.minimumStartTime = minimumStartTime;
        }
    }

    /**
     * Finds availability rules that match a specific day.
     * A rule matches if:
     * 1. The rule overlaps with the day
     * 2. The day of week matches
     */
    private List<AvailabilityRule> findMatchingRulesForDay(
            Instant dayStartInstant, Instant dayEndInstant, LocalDate suggestedDate, ZoneId zoneId) {
        List<AvailabilityRule> activeRules = repository.findByRuleStatus(AvailabilityRule.RuleStatus.ACTIVE);

        return activeRules.stream()
                .filter(rule -> {
                    // Check if rule overlaps with the day
                    // Two intervals overlap if: ruleStart <= dayEnd AND ruleEnd >= dayStart
                    Instant ruleStart = rule.getRuleStartInstant();
                    Instant ruleEnd = rule.getRuleEndInstant();

                    boolean ruleOverlapsDay = (ruleStart.isBefore(dayEndInstant) || ruleStart.equals(dayEndInstant))
                            && (ruleEnd.isAfter(dayStartInstant) || ruleEnd.equals(dayStartInstant));

                    if (!ruleOverlapsDay) {
                        return false;
                    }

                    // Get day(s) of week in the rule's timezone
                    // The client's day (dayStartInstant to dayEndInstant) may span two different
                    // calendar days in the rule's timezone, so we need to check both
                    ZoneId ruleZoneId = ZoneId.of(TimezoneEntry.getById(rule.getTimezoneId()).getGmtOffset());

                    // Get the date at the start of the client's day in the rule's timezone
                    LocalDate startDateInRuleTimezone = dayStartInstant.atZone(ruleZoneId).toLocalDate();
                    // Get the date at the end of the client's day in the rule's timezone
                    LocalDate endDateInRuleTimezone = dayEndInstant.atZone(ruleZoneId).toLocalDate();

                    // Check if at least one of these days matches the rule's days of week
                    DayOfWeek startDayOfWeek = startDateInRuleTimezone.getDayOfWeek();
                    DayOfWeek endDayOfWeek = endDateInRuleTimezone.getDayOfWeek();

                    boolean startDayMatches = containsDay(rule.getDaysOfWeekAsInt(), startDayOfWeek.getValue());
                    boolean endDayMatches = containsDay(rule.getDaysOfWeekAsInt(), endDayOfWeek.getValue());

                    return startDayMatches || endDayMatches;
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculates available time ranges for a given day by considering availability
     * rules,
     * overrides, and existing bookings.
     */
    private List<TimeRange> calculateAvailableTimeRanges(
            Instant dayStartInstant,
            Instant dayEndInstant,
            LocalDate suggestedDate,
            ZoneId zoneId,
            Instant minimumStartTime) {

        List<TimeRange> allAvailableRanges =
                calculateAvailableTimeRangesByRulesAndOverrides(
                        dayStartInstant, dayEndInstant, suggestedDate,
                        zoneId, minimumStartTime);

        // Subtract booked time ranges from all available ranges
        List<Booking> existingBookings = bookingRepository.findBookingsByStatusAndTimeRange(
                List.of(BookingStatus.PENDING_APPROVAL, BookingStatus.CONFIRMED),
                dayStartInstant,
                dayEndInstant);

        allAvailableRanges = subtractBookedRangesFromAll(allAvailableRanges, existingBookings);

        return allAvailableRanges;
    }

    private List<TimeRange> calculateAvailableTimeRangesForUpdate(
            Instant dayStartInstant,
            Instant dayEndInstant,
            LocalDate suggestedDate,
            ZoneId zoneId,
            Instant minimumStartTime,
            Booking bookingToUpdate) {

        List<TimeRange> allAvailableRanges =
                calculateAvailableTimeRangesByRulesAndOverrides(
                        dayStartInstant, dayEndInstant, suggestedDate,
                        zoneId, minimumStartTime);

        // Get current booking excluding updating one - current booking time should not be substructed from available time
        // As if this booking was canceled and recreated.
        List<Booking> existingBookingsExcludeUpdatingItem = bookingRepository.findBookingsByStatusAndTimeRange(
                        List.of(BookingStatus.PENDING_APPROVAL, BookingStatus.CONFIRMED),
                        dayStartInstant,
                        dayEndInstant).stream()
                .filter(booking -> !booking.getId().equals(bookingToUpdate.getId())).
                collect(Collectors.toList());

        // Subtract booked time ranges from all available ranges
        allAvailableRanges = subtractBookedRangesFromAll(allAvailableRanges, existingBookingsExcludeUpdatingItem);

        return allAvailableRanges;
    }

    private List<TimeRange> calculateAvailableTimeRangesByRulesAndOverrides(
            Instant dayStartInstant,
            Instant dayEndInstant,
            LocalDate suggestedDate,
            ZoneId zoneId,
            Instant minimumStartTime) {
        // Find rules that match the day
        List<AvailabilityRule> matchingRules = findMatchingRulesForDay(
                dayStartInstant, dayEndInstant, suggestedDate, zoneId);

        // Get unavailable overrides that reduce availability
        List<AvailabilityOverride> unavailableOverrides = overrideRepository.findOverlappingUnavailableOverrides(
                dayStartInstant, dayEndInstant, OverrideStatus.ACTIVE);

        // Collect working hours from all matching rules and find overlap with client's
        // day
        // Note: Different rules' working hours don't overlap, so we process each
        // separately
        List<TimeRange> ruleRanges = new ArrayList<>();
        for (AvailabilityRule rule : matchingRules) {
            ZoneId ruleZoneId = ZoneId.of(TimezoneEntry.getById(rule.getTimezoneId()).getGmtOffset());
            LocalTime availableStart = rule.getAvailableStartTime();
            LocalTime availableEnd = rule.getAvailableEndTime();

            // The client's day range may span multiple calendar days in the rule's timezone
            // We need to check each calendar day in the rule's timezone that overlaps with
            // the client's day
            LocalDate startDateInRuleTimezone = dayStartInstant.atZone(ruleZoneId).toLocalDate();
            LocalDate endDateInRuleTimezone = dayEndInstant.atZone(ruleZoneId).toLocalDate();

            // Iterate through each calendar day in the rule's timezone that the client's
            // day spans
            LocalDate candidateDate = startDateInRuleTimezone;
            while (!candidateDate.isAfter(endDateInRuleTimezone)) {
                // Check if this day of the week is in the rule's working days
                DayOfWeek dayOfWeek = candidateDate.getDayOfWeek();
                if (containsDay(rule.getDaysOfWeekAsInt(), dayOfWeek.getValue())) {
                    // Calculate working hours for this specific day in the rule's timezone
                    LocalDateTime candidateStartDateTime = candidateDate.atTime(availableStart);
                    LocalDateTime candidateEndDateTime = candidateDate.atTime(availableEnd);
                    Instant candidateStartInstant = candidateStartDateTime.atZone(ruleZoneId).toInstant();
                    Instant candidateEndInstant = candidateEndDateTime.atZone(ruleZoneId).toInstant();

                    if (candidateStartInstant.isAfter(rule.getRuleEndInstant())) {
                        //the case when rule overlaps day, but ends before start time in this day
                        break;
                    }

                    // Find overlap between client's day and this day's working hours
                    TimeRange ruleOverlap = findFutureOverlap(
                            new TimeRange(dayStartInstant, dayEndInstant),
                            new TimeRange(candidateStartInstant, candidateEndInstant),
                            minimumStartTime);

                    if (ruleOverlap != null) {
                        ruleRanges.add(ruleOverlap);
                    }
                }

                // Move to the next day in the rule's timezone
                candidateDate = candidateDate.plusDays(1);
            }
        }

        // Subtract unavailable override ranges from rule ranges
        List<TimeRange> allAvailableRanges = subtractUnavailableRangesFromAll(ruleRanges, unavailableOverrides);

        // Always check for availability overrides with isAvailable = true
        // These overrides extend availability beyond normal rules
        List<AvailabilityOverride> availableOverrides = overrideRepository.findOverlappingAvailableOverrides(
                dayStartInstant, dayEndInstant, OverrideStatus.ACTIVE);

        for (AvailabilityOverride override : availableOverrides) {
            // Calculate overlap between override and client's day (filter past intervals if
            // today)
            TimeRange overrideOverlap = findFutureOverlap(
                    new TimeRange(dayStartInstant, dayEndInstant),
                    new TimeRange(override.getOverideStartInstant(), override.getOverrideEndInstant()),
                    minimumStartTime);

            if (overrideOverlap != null) {
                allAvailableRanges.add(overrideOverlap);
            }
        }

        return allAvailableRanges;
    }

    private List<BookingSuggestion> generateSlots(TimeRange range, int sessionDurationMin, int sessionBufferTimeMin,
                                                  SuggestionContext context) {
        return generateSlotsInternal(
                range,
                sessionDurationMin,
                sessionBufferTimeMin,
                context.settings.getBookingSlotsInterval(),
                null,
                context);
    }

    private List<BookingSuggestion> generateSlotsForUpdate(
            TimeRange range,
            Booking bookingToUpdate,
            SuggestionContext context
    ) {
        return generateSlotsInternal(
                range,
                bookingToUpdate.getSessionDurationMinutes(),
                bookingToUpdate.getSessionBufferMinutes(),
                context.settings.getBookingSlotsInterval(),
                bookingToUpdate.getStartTime(),
                context);
    }

    private List<BookingSuggestion> generateSlotsInternal(
            TimeRange range,
            int sessionDurationMin,
            int sessionBufferTimeMin,
            int slotIntervalMinutes,
            Instant skipStartTime,
            SuggestionContext context) {
        List<BookingSuggestion> slots = new ArrayList<>();
        Instant currentStart = range.getStart();
        while (true) {
            if (context.settings.isRoundBookingSuggestions()) {
                currentStart = roundUpToQuarterHour(currentStart, context.zoneId);
            }
            // Check if next slot start would exceed the range
            if (currentStart.isAfter(range.getEnd()) || currentStart.equals(range.getEnd())) {
                break;
            }
            if (skipStartTime != null && currentStart.equals(skipStartTime)) {
                // Skip current time from suggestion
                currentStart = currentStart.plusSeconds(slotIntervalMinutes * 60L);
                continue;
            }

            Instant slotEnd = currentStart.plusSeconds(sessionDurationMin * 60L);
            Instant slotWithBufferEnd = slotEnd.plusSeconds(sessionBufferTimeMin * 60L);

            // Check if slot end exceeds the range end
            if (slotWithBufferEnd.isAfter(range.getEnd())) {
                break;
            }

            BookingSuggestion suggestion = new BookingSuggestion();
            suggestion.setStartTime(currentStart);
            suggestion.setEndTime(slotEnd);
            suggestion.setStartTimeInstant(currentStart);
            slots.add(suggestion);

            // Move to next slot start (current start + interval)
            currentStart = currentStart.plusSeconds(slotIntervalMinutes * 60L);
        }

        return slots;
    }

    private TimeRange findOverlap(TimeRange range1, TimeRange range2) {
        Instant overlapStart = range1.getStart().isAfter(range2.getStart()) ? range1.getStart() : range2.getStart();
        Instant overlapEnd = range1.getEnd().isBefore(range2.getEnd()) ? range1.getEnd() : range2.getEnd();

        // Check if there's actually an overlap (overlapStart must be before overlapEnd)
        boolean hasOverlap = overlapStart.isBefore(overlapEnd);
        if (!hasOverlap) {
            return null;
        }

        return new TimeRange(overlapStart, overlapEnd);
    }

    private TimeRange findFutureOverlap(TimeRange range1, TimeRange range2, Instant minimumStartTime) {
        Instant overlapStart = range1.getStart().isAfter(range2.getStart()) ? range1.getStart() : range2.getStart();
        Instant overlapEnd = range1.getEnd().isBefore(range2.getEnd()) ? range1.getEnd() : range2.getEnd();

        // Check if there's actually an overlap (overlapStart must be before overlapEnd)
        boolean hasOverlap = overlapStart.isBefore(overlapEnd);
        if (!hasOverlap) {
            return null;
        }

        // If minimumStartTime is provided (for today's bookings), ensure overlap starts
        // after it
        if (minimumStartTime != null) {
            if (overlapStart.isBefore(minimumStartTime)) {
                // Adjust overlap start to minimumStartTime if it starts earlier
                overlapStart = minimumStartTime;
                // Check if there's still valid overlap after adjustment
                if (!overlapStart.isBefore(overlapEnd)) {
                    return null;
                }
            }
        } else {
            // For future dates, ensure overlap doesn't start in the past
            Instant now = Instant.now();
            if (overlapStart.isBefore(now) || overlapStart.equals(now)) {
                return null;
            }
        }

        return new TimeRange(overlapStart, overlapEnd);
    }

    /**
     * Subtracts booked time ranges from all available time ranges.
     * Returns a list of remaining available time ranges after excluding booked
     * periods.
     */
    private List<TimeRange> subtractBookedRangesFromAll(
            List<TimeRange> availableRanges,
            List<Booking> bookingsToSubtract) {

        if (bookingsToSubtract.isEmpty()) {
            return availableRanges;
        }

        // Subtract booked time ranges from all available ranges
        List<TimeRange> bookedRanges = bookingsToSubtract.stream()
                .map(booking -> new TimeRange(booking.getStartTime(), booking.getEndTime()))
                .collect(Collectors.toList());

        List<TimeRange> result = new ArrayList<>(availableRanges);

        // Apply each booked range to all current available ranges
        for (TimeRange bookedRange : bookedRanges) {
            List<TimeRange> newResult = new ArrayList<>();
            for (TimeRange currentRange : result) {
                // Subtract booked range from current range
                List<TimeRange> remaining = subtractRange(currentRange, bookedRange);
                newResult.addAll(remaining);
            }
            result = newResult;
        }

        return result;
    }

    /**
     * Subtracts unavailable override ranges from all available time ranges.
     * Returns a list of remaining available time ranges after excluding unavailable
     * periods.
     */
    private List<TimeRange> subtractUnavailableRangesFromAll(
            List<TimeRange> availableRanges,
            List<AvailabilityOverride> unavailableOverrides) {
        if (unavailableOverrides.isEmpty()) {
            return availableRanges;
        }

        List<TimeRange> unavailableRanges = unavailableOverrides.stream()
                .map(override -> new TimeRange(
                        override.getOverideStartInstant(),
                        override.getOverrideEndInstant()))
                .collect(Collectors.toList());

        List<TimeRange> result = new ArrayList<>(availableRanges);

        // Apply each unavailable range to all current available ranges
        for (TimeRange unavailableRange : unavailableRanges) {
            List<TimeRange> newResult = new ArrayList<>();
            for (TimeRange currentRange : result) {
                // Subtract unavailable range from current range
                List<TimeRange> remaining = subtractRange(currentRange, unavailableRange);
                newResult.addAll(remaining);
            }
            result = newResult;
        }

        return result;
    }

    /**
     * Subtracts a range from another range, returning the remaining parts.
     * Can return 0, 1, or 2 ranges depending on how the ranges overlap.
     */
    private List<TimeRange> subtractRange(TimeRange original, TimeRange toSubtract) {
        List<TimeRange> result = new ArrayList<>();

        // Check if ranges overlap (no minimum start time restriction for subtraction)
        TimeRange overlap = findOverlap(original, toSubtract);
        if (overlap == null) {
            // No overlap, return original range
            result.add(original);
            return result;
        }

        // If overlap completely covers original (overlap equals original), return empty
        // list
        if (overlap.getStart().equals(original.getStart()) && overlap.getEnd().equals(original.getEnd())) {
            return result;
        }

        // Check if there's a part before the overlap
        if (original.getStart().isBefore(overlap.getStart())) {
            result.add(new TimeRange(original.getStart(), overlap.getStart()));
        }

        // Check if there's a part after the overlap
        if (original.getEnd().isAfter(overlap.getEnd())) {
            result.add(new TimeRange(overlap.getEnd(), original.getEnd()));
        }

        return result;
    }

    private static boolean containsDay(int[] daysOfWeekAsInt, int dayValue) {
        return Arrays.stream(daysOfWeekAsInt)
                .anyMatch(day -> day == dayValue);
    }

    private static Instant roundUpToQuarterHour(Instant instant, ZoneId zoneId) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, zoneId);
        int minute = localDateTime.getMinute();
        int mod = minute % 15;
        int addMinutes;
        if (mod == 0 && localDateTime.getSecond() == 0 && localDateTime.getNano() == 0) {
            addMinutes = 0;
        } else {
            addMinutes = 15 - mod;
        }
        LocalDateTime rounded = localDateTime
                .plusMinutes(addMinutes)
                .withSecond(0)
                .withNano(0);
        return rounded.atZone(zoneId).toInstant();
    }

    private static class TimeRange {
        private final Instant start;
        private final Instant end;

        TimeRange(Instant start, Instant end) {
            this.start = start;
            this.end = end;
        }

        Instant getStart() {
            return start;
        }

        Instant getEnd() {
            return end;
        }
    }
}
