-- Add prices column as JSONB to session_types table
ALTER TABLE session_types ADD COLUMN prices JSONB;

