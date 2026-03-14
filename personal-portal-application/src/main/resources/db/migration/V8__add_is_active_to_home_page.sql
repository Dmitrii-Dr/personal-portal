-- Add is_active flag to home_page table
ALTER TABLE home_page ADD COLUMN is_active boolean NOT NULL DEFAULT false;
