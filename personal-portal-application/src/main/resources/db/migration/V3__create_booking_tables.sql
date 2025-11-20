-- Create booking_settings table (singleton table for booking configuration)
CREATE TABLE booking_settings (
    id BIGSERIAL PRIMARY KEY,
    booking_slots_interval INTEGER NOT NULL,
    booking_cancelation_interval INTEGER NOT NULL,
    booking_updating_interval INTEGER NOT NULL,
    default_timezone VARCHAR(50) NOT NULL,
    default_utc_offset VARCHAR(10) NOT NULL
);

-- Create session_types table
CREATE TABLE session_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    description VARCHAR(2000) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    buffer_minutes INTEGER NOT NULL
);

-- Create availability_rules table
CREATE TABLE availability_rules (
    id BIGSERIAL PRIMARY KEY,
    days_of_week INTEGER[] NOT NULL,
    available_start_time TIME NOT NULL,
    available_end_time TIME NOT NULL,
    rule_start_time TIMESTAMPTZ,
    rule_end_time TIMESTAMPTZ,
    timezone VARCHAR(50),
    utc_offset VARCHAR(10),
    rule_status INTEGER NOT NULL
);

-- Create availability_overrides table
CREATE TABLE availability_overrides (
    id BIGSERIAL PRIMARY KEY,
    override_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN NOT NULL
);

-- Create bookings table
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    client_id UUID NOT NULL,
    session_type_id BIGINT NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(50) NOT NULL,
    client_message VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_bookings_client FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_bookings_session_type FOREIGN KEY (session_type_id) REFERENCES session_types(id) ON DELETE RESTRICT
);

-- Create indexes for better query performance
CREATE INDEX idx_bookings_client_id ON bookings(client_id);
CREATE INDEX idx_bookings_session_type_id ON bookings(session_type_id);
CREATE INDEX idx_bookings_start_time ON bookings(start_time);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_availability_rules_status ON availability_rules(rule_status);
CREATE INDEX idx_availability_rules_rule_start_time ON availability_rules(rule_start_time);
CREATE INDEX idx_availability_rules_rule_end_time ON availability_rules(rule_end_time);
CREATE INDEX idx_availability_overrides_override_date ON availability_overrides(override_date);

