-- Add email_notification_enabled column to user_settings table
ALTER TABLE user_settings ADD COLUMN email_notification_enabled BOOLEAN NOT NULL DEFAULT true;

