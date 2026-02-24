-- Create contact table for home_page
CREATE TABLE home_page_contact (
    home_page_id UUID NOT NULL,
    platform VARCHAR(255) NOT NULL,
    value VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    PRIMARY KEY (home_page_id, platform, value),
    FOREIGN KEY (home_page_id) REFERENCES home_page(home_page_id) ON DELETE CASCADE
);

