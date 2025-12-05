-- Add active flag to session_types table
ALTER TABLE session_types ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;

-- Add index on active column for query performance
CREATE INDEX idx_session_types_active ON session_types(active);

