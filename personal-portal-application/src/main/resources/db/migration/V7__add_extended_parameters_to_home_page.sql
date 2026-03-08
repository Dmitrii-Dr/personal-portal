-- Add extended_parameters as jsonb to home_page table
ALTER TABLE home_page ADD COLUMN extended_parameters jsonb;
