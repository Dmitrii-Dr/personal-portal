-- Add or rename booking_first_slot_interval column to booking_settings table
-- If booking_today_first_slot_interval exists, rename it; otherwise add the new column

DO $$
BEGIN
    -- Check if booking_today_first_slot_interval column exists and rename it
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'booking_settings' 
        AND column_name = 'booking_today_first_slot_interval'
    ) THEN
        ALTER TABLE booking_settings 
            RENAME COLUMN booking_today_first_slot_interval TO booking_first_slot_interval;
    ELSE
        -- Add the new column if it doesn't exist
        ALTER TABLE booking_settings 
            ADD COLUMN booking_first_slot_interval INTEGER NOT NULL DEFAULT 5;
        
        -- Remove the default value after adding the column
        -- (The DEFAULT is needed for existing rows, but we don't want it for new inserts)
        ALTER TABLE booking_settings 
            ALTER COLUMN booking_first_slot_interval DROP DEFAULT;
    END IF;
END $$;


