-- Add denormalized session type data columns to bookings table
ALTER TABLE bookings ADD COLUMN session_name VARCHAR(200);
ALTER TABLE bookings ADD COLUMN session_duration_minutes INTEGER;
ALTER TABLE bookings ADD COLUMN session_buffer_minutes INTEGER;
ALTER TABLE bookings ADD COLUMN session_prices JSONB;
ALTER TABLE bookings ADD COLUMN session_description VARCHAR(2000);

-- Migrate existing bookings: populate new columns from referenced session_type_id
UPDATE bookings b
SET 
    session_name = st.name,
    session_duration_minutes = st.duration_minutes,
    session_buffer_minutes = st.buffer_minutes,
    session_prices = COALESCE(st.prices, '{}'::jsonb),
    session_description = st.description
FROM session_types st
WHERE b.session_type_id = st.id;

-- Make new columns NOT NULL after migration
ALTER TABLE bookings ALTER COLUMN session_name SET NOT NULL;
ALTER TABLE bookings ALTER COLUMN session_duration_minutes SET NOT NULL;
ALTER TABLE bookings ALTER COLUMN session_buffer_minutes SET NOT NULL;
ALTER TABLE bookings ALTER COLUMN session_prices SET NOT NULL;

-- Remove foreign key constraint
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS fk_bookings_session_type;

-- Remove session_type_id column
ALTER TABLE bookings DROP COLUMN IF EXISTS session_type_id;

-- Remove index on session_type_id if it exists
DROP INDEX IF EXISTS idx_bookings_session_type_id;

