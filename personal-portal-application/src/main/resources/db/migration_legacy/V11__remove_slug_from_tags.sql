-- Drop index on tags.slug
DROP INDEX IF EXISTS idx_tags_slug;

-- Remove slug column from tags table
ALTER TABLE tags DROP COLUMN IF EXISTS slug;

