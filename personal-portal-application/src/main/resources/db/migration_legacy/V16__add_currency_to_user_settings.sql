-- Add currency column to user_settings table
ALTER TABLE user_settings ADD COLUMN currency VARCHAR(10) NOT NULL DEFAULT 'RUB';

