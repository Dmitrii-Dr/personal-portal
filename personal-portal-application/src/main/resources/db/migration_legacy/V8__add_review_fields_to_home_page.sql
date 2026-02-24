-- Add review_message column to home_page table
ALTER TABLE home_page ADD COLUMN review_message TEXT;

-- Add review_media_ids as UUID array column
ALTER TABLE home_page ADD COLUMN review_media_ids UUID[];

