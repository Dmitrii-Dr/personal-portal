ALTER TABLE booking_settings
    ADD COLUMN round_booking_suggestions BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE booking_settings
    ALTER COLUMN round_booking_suggestions DROP DEFAULT;
