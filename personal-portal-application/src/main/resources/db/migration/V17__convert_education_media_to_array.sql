ALTER TABLE home_page
    ADD COLUMN education_media_ids UUID[] NOT NULL DEFAULT ARRAY[]::UUID[];

UPDATE home_page
SET education_media_ids = ARRAY[education_media_id]::UUID[]
WHERE education_media_id IS NOT NULL;

ALTER TABLE home_page
    DROP COLUMN education_media_id;
