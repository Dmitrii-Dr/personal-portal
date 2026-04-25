ALTER TABLE booking_settings
ADD COLUMN max_pending_bookings INTEGER NOT NULL DEFAULT 3;
