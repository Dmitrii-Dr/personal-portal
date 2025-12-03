-- Make description column nullable in session_types table
ALTER TABLE session_types ALTER COLUMN description DROP NOT NULL;

