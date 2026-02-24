-- Booking tables (final schema)

-- Booking settings table (singleton)
CREATE TABLE booking_settings (
    id BIGSERIAL PRIMARY KEY,
    booking_slots_interval INTEGER NOT NULL,
    booking_cancelation_interval INTEGER NOT NULL,
    booking_updating_interval INTEGER NOT NULL,
    default_utc_offset VARCHAR(10) NOT NULL,
    booking_first_slot_interval INTEGER NOT NULL,
    default_timezone_id INTEGER NOT NULL,
    round_booking_suggestions BOOLEAN NOT NULL
);

-- Session types table
CREATE TABLE session_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    description VARCHAR(2000),
    duration_minutes INTEGER NOT NULL,
    buffer_minutes INTEGER NOT NULL,
    prices JSONB,
    active BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX idx_session_types_active ON session_types(active);

-- Availability rules table
CREATE TABLE availability_rules (
    id BIGSERIAL PRIMARY KEY,
    days_of_week INTEGER[] NOT NULL,
    available_start_time TIME NOT NULL,
    available_end_time TIME NOT NULL,
    rule_start_time TIMESTAMPTZ,
    rule_end_time TIMESTAMPTZ,
    timezone_id INTEGER NOT NULL,
    utc_offset VARCHAR(10),
    rule_status INTEGER NOT NULL
);

-- Availability overrides table
CREATE TABLE availability_overrides (
    id BIGSERIAL PRIMARY KEY,
    override_start_time TIMESTAMPTZ NOT NULL,
    override_end_time TIMESTAMPTZ NOT NULL,
    timezone_id INTEGER,
    utc_offset VARCHAR(10),
    override_status VARCHAR(50) NOT NULL
);

-- Bookings table (denormalized session data)
CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    client_id UUID NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(50) NOT NULL,
    client_message VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    session_name VARCHAR(200) NOT NULL,
    session_duration_minutes INTEGER NOT NULL,
    session_buffer_minutes INTEGER NOT NULL,
    session_prices JSONB NOT NULL,
    session_description VARCHAR(2000),
    CONSTRAINT fk_bookings_client FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_bookings_client_id ON bookings(client_id);
CREATE INDEX idx_bookings_start_time ON bookings(start_time);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_availability_rules_status ON availability_rules(rule_status);
CREATE INDEX idx_availability_rules_rule_start_time ON availability_rules(rule_start_time);
CREATE INDEX idx_availability_rules_rule_end_time ON availability_rules(rule_end_time);
CREATE INDEX idx_availability_overrides_start_time ON availability_overrides(override_start_time);
CREATE INDEX idx_availability_overrides_end_time ON availability_overrides(override_end_time);
CREATE INDEX idx_availability_overrides_status ON availability_overrides(override_status);
