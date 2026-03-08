-- Rename welcome_media_id to welcome_right_media_id
ALTER TABLE home_page RENAME COLUMN welcome_media_id TO welcome_right_media_id;

-- Add welcome_left_media_id to home_page table
ALTER TABLE home_page ADD COLUMN welcome_left_media_id UUID;
