-- Add last_password_reset_date column to users table
ALTER TABLE users ADD COLUMN last_password_reset_date TIMESTAMPTZ;

