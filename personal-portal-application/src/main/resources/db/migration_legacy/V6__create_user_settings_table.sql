-- Create user_settings table
CREATE TABLE user_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    timezone VARCHAR(50) NOT NULL,
    language VARCHAR(10) NOT NULL,
    CONSTRAINT fk_user_settings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
