-- Add slug to agreements table
ALTER TABLE agreements ADD COLUMN slug VARCHAR(255);
UPDATE agreements SET slug = name;
ALTER TABLE agreements ALTER COLUMN slug SET NOT NULL;
ALTER TABLE agreements ADD CONSTRAINT uc_agreements_slug UNIQUE (slug);

-- Add slug to user_signed_agreements table
ALTER TABLE user_signed_agreements ADD COLUMN slug VARCHAR(255);
UPDATE user_signed_agreements SET slug = name;
ALTER TABLE user_signed_agreements ALTER COLUMN slug SET NOT NULL;
