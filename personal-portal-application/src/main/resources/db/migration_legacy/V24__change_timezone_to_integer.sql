-- Migration V24: Change timezone to integer ID

-- 1. Add timezone_id column to tables
ALTER TABLE user_settings ADD COLUMN timezone_id INTEGER;
ALTER TABLE availability_rules ADD COLUMN timezone_id INTEGER;
ALTER TABLE availability_overrides ADD COLUMN timezone_id INTEGER;
ALTER TABLE booking_settings ADD COLUMN default_timezone_id INTEGER;

-- 2. Update timezone_id based on existing string value 
-- Mapping TimezoneEntry values from core
-- INTERNATIONAL_DATE_LINE_WEST(1, "International Date Line West", "GMT-12:00")
-- AMERICAN_SAMOA(2, "American Samoa", "GMT-11:00")
-- HAWAII(3, "Hawaii", "GMT-10:00")
-- ALASKA(4, "Alaska", "GMT-09:00")
-- PACIFIC_TIME(5, "Pacific Time (US & Canada)", "GMT-08:00")
-- MOUNTAIN_TIME(6, "Mountain Time (US & Canada)", "GMT-07:00")
-- CENTRAL_TIME(7, "Central Time (US & Canada)", "GMT-06:00")
-- EASTERN_TIME(8, "Eastern Time (US & Canada)", "GMT-05:00")
-- ATLANTIC_TIME(9, "Atlantic Time (Canada)", "GMT-04:00")
-- BUENOS_AIRES_BRASILIA(10, "Buenos Aires, Brasilia", "GMT-03:00")
-- MID_ATLANTIC(11, "Mid-Atlantic", "GMT-02:00")
-- AZORES(12, "Azores", "GMT-01:00")
-- LONDON_DUBLIN_LISBON(13, "London, Dublin, Lisbon", "GMT+00:00")
-- PARIS_BERLIN_ROME_MADRID(14, "Paris, Berlin, Rome, Madrid", "GMT+01:00")
-- ATHENS_CAIRO_ISTANBUL(15, "Athens, Cairo, Istanbul", "GMT+02:00")
-- MOSCOW_KUWAIT_RIYADH(16, "Moscow, Kuwait, Riyadh", "GMT+03:00")
-- DUBAI_ABU_DHABI_BAKU(17, "Dubai, Abu Dhabi, Baku", "GMT+04:00")
-- KARACHI_TASHKENT(18, "Karachi, Tashkent", "GMT+05:00")
-- MUMBAI_KOLKATA_NEW_DELHI(19, "Mumbai, Kolkata, New Delhi", "GMT+05:30")
-- DHAKA_ALMATY(20, "Dhaka, Almaty", "GMT+06:00")
-- BANGKOK_JAKARTA_HANOI(21, "Bangkok, Jakarta, Hanoi", "GMT+07:00")
-- SINGAPORE_HONG_KONG_BEIJING(22, "Singapore, Hong Kong, Beijing", "GMT+08:00")
-- TOKYO_SEOUL_OSAKA(23, "Tokyo, Seoul, Osaka", "GMT+09:00")
-- SYDNEY_MELBOURNE_BRISBANE(24, "Sydney, Melbourne, Brisbane", "GMT+10:00")
-- SOLOMON_ISLANDS_NEW_CALEDONIA(25, "Solomon Islands, New Caledonia", "GMT+11:00")
-- AUCKLAND_FIJI_WELLINGTON(26, "Auckland, Fiji, Wellington", "GMT+12:00")
-- TONGA_SAMOA(27, "Tonga, Samoa", "GMT+13:00")

-- Since old values could be Display Name OR GMT Offset (TimezoneServiceImpl logic was mixed/lenient, but mostly used Offset internally or passed around strings),
-- we map both Display Name AND GMT Offset to be safe.
-- Also handle nulls by leaving timezone_id NULL (or handle carefully). 
-- Tables have NOT NULL constraint for timezone?
-- UserSettings: timezone nullable=false
-- AvailabilityRule: timezone_id nullable? nullable default true. But previously timezone might be NOT NULL?
-- AvailabilityRule: @Column(name = "timezone", nullable = false) -> was nullable=false? Let's check. Yes, likely required for rules.
-- AvailabilityOverride: timezone length=50, nullable? default true.

-- Update user_settings
UPDATE user_settings 
SET timezone_id = CASE 
    WHEN timezone = 'International Date Line West' OR timezone = 'GMT-12:00' THEN 1
    WHEN timezone = 'American Samoa' OR timezone = 'GMT-11:00' THEN 2
    WHEN timezone = 'Hawaii' OR timezone = 'GMT-10:00' THEN 3
    WHEN timezone = 'Alaska' OR timezone = 'GMT-09:00' THEN 4
    WHEN timezone = 'Pacific Time (US & Canada)' OR timezone = 'GMT-08:00' THEN 5
    WHEN timezone = 'Mountain Time (US & Canada)' OR timezone = 'GMT-07:00' THEN 6
    WHEN timezone = 'Central Time (US & Canada)' OR timezone = 'GMT-06:00' THEN 7
    WHEN timezone = 'Eastern Time (US & Canada)' OR timezone = 'GMT-05:00' THEN 8
    WHEN timezone = 'Atlantic Time (Canada)' OR timezone = 'GMT-04:00' THEN 9
    WHEN timezone = 'Buenos Aires, Brasilia' OR timezone = 'GMT-03:00' THEN 10
    WHEN timezone = 'Mid-Atlantic' OR timezone = 'GMT-02:00' THEN 11
    WHEN timezone = 'Azores' OR timezone = 'GMT-01:00' THEN 12
    WHEN timezone = 'London, Dublin, Lisbon' OR timezone = 'GMT+00:00' OR timezone = 'UTC' OR timezone = 'GMT' THEN 13
    WHEN timezone = 'Paris, Berlin, Rome, Madrid' OR timezone = 'GMT+01:00' THEN 14
    WHEN timezone = 'Athens, Cairo, Istanbul' OR timezone = 'GMT+02:00' THEN 15
    WHEN timezone = 'Moscow, Kuwait, Riyadh' OR timezone = 'GMT+03:00' THEN 16
    WHEN timezone = 'Dubai, Abu Dhabi, Baku' OR timezone = 'GMT+04:00' THEN 17
    WHEN timezone = 'Karachi, Tashkent' OR timezone = 'GMT+05:00' THEN 18
    WHEN timezone = 'Mumbai, Kolkata, New Delhi' OR timezone = 'GMT+05:30' THEN 19
    WHEN timezone = 'Dhaka, Almaty' OR timezone = 'GMT+06:00' THEN 20
    WHEN timezone = 'Bangkok, Jakarta, Hanoi' OR timezone = 'GMT+07:00' THEN 21
    WHEN timezone = 'Singapore, Hong Kong, Beijing' OR timezone = 'GMT+08:00' THEN 22
    WHEN timezone = 'Tokyo, Seoul, Osaka' OR timezone = 'GMT+09:00' THEN 23
    WHEN timezone = 'Sydney, Melbourne, Brisbane' OR timezone = 'GMT+10:00' THEN 24
    WHEN timezone = 'Solomon Islands, New Caledonia' OR timezone = 'GMT+11:00' THEN 25
    WHEN timezone = 'Auckland, Fiji, Wellington' OR timezone = 'GMT+12:00' THEN 26
    WHEN timezone = 'Tonga, Samoa' OR timezone = 'GMT+13:00' THEN 27
    ELSE 13 -- Default to UTC (13) for any unmatched values
END;

-- Update availability_rules
UPDATE availability_rules 
SET timezone_id = CASE 
    WHEN timezone = 'International Date Line West' OR timezone = 'GMT-12:00' THEN 1
    WHEN timezone = 'American Samoa' OR timezone = 'GMT-11:00' THEN 2
    WHEN timezone = 'Hawaii' OR timezone = 'GMT-10:00' THEN 3
    WHEN timezone = 'Alaska' OR timezone = 'GMT-09:00' THEN 4
    WHEN timezone = 'Pacific Time (US & Canada)' OR timezone = 'GMT-08:00' THEN 5
    WHEN timezone = 'Mountain Time (US & Canada)' OR timezone = 'GMT-07:00' THEN 6
    WHEN timezone = 'Central Time (US & Canada)' OR timezone = 'GMT-06:00' THEN 7
    WHEN timezone = 'Eastern Time (US & Canada)' OR timezone = 'GMT-05:00' THEN 8
    WHEN timezone = 'Atlantic Time (Canada)' OR timezone = 'GMT-04:00' THEN 9
    WHEN timezone = 'Buenos Aires, Brasilia' OR timezone = 'GMT-03:00' THEN 10
    WHEN timezone = 'Mid-Atlantic' OR timezone = 'GMT-02:00' THEN 11
    WHEN timezone = 'Azores' OR timezone = 'GMT-01:00' THEN 12
    WHEN timezone = 'London, Dublin, Lisbon' OR timezone = 'GMT+00:00' THEN 13
    WHEN timezone = 'Paris, Berlin, Rome, Madrid' OR timezone = 'GMT+01:00' THEN 14
    WHEN timezone = 'Athens, Cairo, Istanbul' OR timezone = 'GMT+02:00' THEN 15
    WHEN timezone = 'Moscow, Kuwait, Riyadh' OR timezone = 'GMT+03:00' THEN 16
    WHEN timezone = 'Dubai, Abu Dhabi, Baku' OR timezone = 'GMT+04:00' THEN 17
    WHEN timezone = 'Karachi, Tashkent' OR timezone = 'GMT+05:00' THEN 18
    WHEN timezone = 'Mumbai, Kolkata, New Delhi' OR timezone = 'GMT+05:30' THEN 19
    WHEN timezone = 'Dhaka, Almaty' OR timezone = 'GMT+06:00' THEN 20
    WHEN timezone = 'Bangkok, Jakarta, Hanoi' OR timezone = 'GMT+07:00' THEN 21
    WHEN timezone = 'Singapore, Hong Kong, Beijing' OR timezone = 'GMT+08:00' THEN 22
    WHEN timezone = 'Tokyo, Seoul, Osaka' OR timezone = 'GMT+09:00' THEN 23
    WHEN timezone = 'Sydney, Melbourne, Brisbane' OR timezone = 'GMT+10:00' THEN 24
    WHEN timezone = 'Solomon Islands, New Caledonia' OR timezone = 'GMT+11:00' THEN 25
    WHEN timezone = 'Auckland, Fiji, Wellington' OR timezone = 'GMT+12:00' THEN 26
    WHEN timezone = 'Tonga, Samoa' OR timezone = 'GMT+13:00' THEN 27
    ELSE 13 -- Default to UTC (13) for any unmatched values
END;

-- Update availability_overrides
UPDATE availability_overrides 
SET timezone_id = CASE 
    WHEN timezone = 'International Date Line West' OR timezone = 'GMT-12:00' THEN 1
    WHEN timezone = 'American Samoa' OR timezone = 'GMT-11:00' THEN 2
    WHEN timezone = 'Hawaii' OR timezone = 'GMT-10:00' THEN 3
    WHEN timezone = 'Alaska' OR timezone = 'GMT-09:00' THEN 4
    WHEN timezone = 'Pacific Time (US & Canada)' OR timezone = 'GMT-08:00' THEN 5
    WHEN timezone = 'Mountain Time (US & Canada)' OR timezone = 'GMT-07:00' THEN 6
    WHEN timezone = 'Central Time (US & Canada)' OR timezone = 'GMT-06:00' THEN 7
    WHEN timezone = 'Eastern Time (US & Canada)' OR timezone = 'GMT-05:00' THEN 8
    WHEN timezone = 'Atlantic Time (Canada)' OR timezone = 'GMT-04:00' THEN 9
    WHEN timezone = 'Buenos Aires, Brasilia' OR timezone = 'GMT-03:00' THEN 10
    WHEN timezone = 'Mid-Atlantic' OR timezone = 'GMT-02:00' THEN 11
    WHEN timezone = 'Azores' OR timezone = 'GMT-01:00' THEN 12
    WHEN timezone = 'London, Dublin, Lisbon' OR timezone = 'GMT+00:00' THEN 13
    WHEN timezone = 'Paris, Berlin, Rome, Madrid' OR timezone = 'GMT+01:00' THEN 14
    WHEN timezone = 'Athens, Cairo, Istanbul' OR timezone = 'GMT+02:00' THEN 15
    WHEN timezone = 'Moscow, Kuwait, Riyadh' OR timezone = 'GMT+03:00' THEN 16
    WHEN timezone = 'Dubai, Abu Dhabi, Baku' OR timezone = 'GMT+04:00' THEN 17
    WHEN timezone = 'Karachi, Tashkent' OR timezone = 'GMT+05:00' THEN 18
    WHEN timezone = 'Mumbai, Kolkata, New Delhi' OR timezone = 'GMT+05:30' THEN 19
    WHEN timezone = 'Dhaka, Almaty' OR timezone = 'GMT+06:00' THEN 20
    WHEN timezone = 'Bangkok, Jakarta, Hanoi' OR timezone = 'GMT+07:00' THEN 21
    WHEN timezone = 'Singapore, Hong Kong, Beijing' OR timezone = 'GMT+08:00' THEN 22
    WHEN timezone = 'Tokyo, Seoul, Osaka' OR timezone = 'GMT+09:00' THEN 23
    WHEN timezone = 'Sydney, Melbourne, Brisbane' OR timezone = 'GMT+10:00' THEN 24
    WHEN timezone = 'Solomon Islands, New Caledonia' OR timezone = 'GMT+11:00' THEN 25
    WHEN timezone = 'Auckland, Fiji, Wellington' OR timezone = 'GMT+12:00' THEN 26
    WHEN timezone = 'Tonga, Samoa' OR timezone = 'GMT+13:00' THEN 27
    ELSE 13 -- Default to UTC (13) for any unmatched values
END;

-- Update booking_settings
UPDATE booking_settings 
SET default_timezone_id = CASE 
    WHEN default_timezone = 'International Date Line West' OR default_timezone = 'GMT-12:00' THEN 1
    WHEN default_timezone = 'American Samoa' OR default_timezone = 'GMT-11:00' THEN 2
    WHEN default_timezone = 'Hawaii' OR default_timezone = 'GMT-10:00' THEN 3
    WHEN default_timezone = 'Alaska' OR default_timezone = 'GMT-09:00' THEN 4
    WHEN default_timezone = 'Pacific Time (US & Canada)' OR default_timezone = 'GMT-08:00' THEN 5
    WHEN default_timezone = 'Mountain Time (US & Canada)' OR default_timezone = 'GMT-07:00' THEN 6
    WHEN default_timezone = 'Central Time (US & Canada)' OR default_timezone = 'GMT-06:00' THEN 7
    WHEN default_timezone = 'Eastern Time (US & Canada)' OR default_timezone = 'GMT-05:00' THEN 8
    WHEN default_timezone = 'Atlantic Time (Canada)' OR default_timezone = 'GMT-04:00' THEN 9
    WHEN default_timezone = 'Buenos Aires, Brasilia' OR default_timezone = 'GMT-03:00' THEN 10
    WHEN default_timezone = 'Mid-Atlantic' OR default_timezone = 'GMT-02:00' THEN 11
    WHEN default_timezone = 'Azores' OR default_timezone = 'GMT-01:00' THEN 12
    WHEN default_timezone = 'London, Dublin, Lisbon' OR default_timezone = 'GMT+00:00' OR default_timezone = 'UTC' OR default_timezone = 'GMT' THEN 13
    WHEN default_timezone = 'Paris, Berlin, Rome, Madrid' OR default_timezone = 'GMT+01:00' THEN 14
    WHEN default_timezone = 'Athens, Cairo, Istanbul' OR default_timezone = 'GMT+02:00' THEN 15
    WHEN default_timezone = 'Moscow, Kuwait, Riyadh' OR default_timezone = 'GMT+03:00' THEN 16
    WHEN default_timezone = 'Dubai, Abu Dhabi, Baku' OR default_timezone = 'GMT+04:00' THEN 17
    WHEN default_timezone = 'Karachi, Tashkent' OR default_timezone = 'GMT+05:00' THEN 18
    WHEN default_timezone = 'Mumbai, Kolkata, New Delhi' OR default_timezone = 'GMT+05:30' THEN 19
    WHEN default_timezone = 'Dhaka, Almaty' OR default_timezone = 'GMT+06:00' THEN 20
    WHEN default_timezone = 'Bangkok, Jakarta, Hanoi' OR default_timezone = 'GMT+07:00' THEN 21
    WHEN default_timezone = 'Singapore, Hong Kong, Beijing' OR default_timezone = 'GMT+08:00' THEN 22
    WHEN default_timezone = 'Tokyo, Seoul, Osaka' OR default_timezone = 'GMT+09:00' THEN 23
    WHEN default_timezone = 'Sydney, Melbourne, Brisbane' OR default_timezone = 'GMT+10:00' THEN 24
    WHEN default_timezone = 'Solomon Islands, New Caledonia' OR default_timezone = 'GMT+11:00' THEN 25
    WHEN default_timezone = 'Auckland, Fiji, Wellington' OR default_timezone = 'GMT+12:00' THEN 26
    WHEN default_timezone = 'Tonga, Samoa' OR default_timezone = 'GMT+13:00' THEN 27
    ELSE 13 -- Default to UTC (13) for any unmatched values
END;

-- 3. Drop old timezone column
ALTER TABLE user_settings DROP COLUMN timezone;
ALTER TABLE availability_rules DROP COLUMN timezone;
ALTER TABLE availability_overrides DROP COLUMN timezone;
ALTER TABLE booking_settings DROP COLUMN default_timezone;

-- 4. Add NOT NULL constraint if applicable
-- For user_settings it was NOT NULL
ALTER TABLE user_settings ALTER COLUMN timezone_id SET NOT NULL;
-- For availability_rules it was NOT NULL
-- Check for NULLs first? If Update failed (e.g. unexpected string), existing rows have NULL.
-- In strict migration we should probably default or error. I'll invoke NOT NULL which might fail if data is dirty.
ALTER TABLE availability_rules ALTER COLUMN timezone_id SET NOT NULL;
-- For booking_settings it was NOT NULL
ALTER TABLE booking_settings ALTER COLUMN default_timezone_id SET NOT NULL;
