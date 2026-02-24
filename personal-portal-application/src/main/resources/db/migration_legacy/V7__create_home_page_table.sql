-- Create home_page table
CREATE TABLE home_page (
    home_page_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    welcome_message TEXT,
    welcome_media_id UUID,
    about_message TEXT,
    about_media_id UUID,
    education_message TEXT,
    education_media_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

