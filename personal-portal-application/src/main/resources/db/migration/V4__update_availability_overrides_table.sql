-- Update availability_overrides table to match entity structure
-- Drop old columns that don't match the entity
ALTER TABLE availability_overrides 
    DROP COLUMN IF EXISTS override_date,
    DROP COLUMN IF EXISTS start_time,
    DROP COLUMN IF EXISTS end_time;

-- Add new columns to match AvailabilityOverride entity
ALTER TABLE availability_overrides 
    ADD COLUMN override_start_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN override_end_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN timezone VARCHAR(50),
    ADD COLUMN utc_offset VARCHAR(10),
    ADD COLUMN override_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

-- Update the default values to be removed after initial migration
-- (The NOT NULL constraint with DEFAULT is needed for existing rows)
ALTER TABLE availability_overrides 
    ALTER COLUMN override_start_time DROP DEFAULT,
    ALTER COLUMN override_end_time DROP DEFAULT,
    ALTER COLUMN override_status DROP DEFAULT;

-- Add index for override_start_time and override_end_time for better query performance
CREATE INDEX IF NOT EXISTS idx_availability_overrides_start_time ON availability_overrides(override_start_time);
CREATE INDEX IF NOT EXISTS idx_availability_overrides_end_time ON availability_overrides(override_end_time);
CREATE INDEX IF NOT EXISTS idx_availability_overrides_status ON availability_overrides(override_status);

-- Drop old index on override_date (if it exists, since we removed that column)
DROP INDEX IF EXISTS idx_availability_overrides_override_date;

