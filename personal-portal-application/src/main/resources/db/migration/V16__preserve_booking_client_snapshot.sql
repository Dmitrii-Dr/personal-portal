ALTER TABLE bookings
    ADD COLUMN client_email VARCHAR(255),
    ADD COLUMN client_first_name VARCHAR(100),
    ADD COLUMN client_last_name VARCHAR(100),
    ADD COLUMN client_phone_number VARCHAR(20);

UPDATE bookings b
SET client_email = u.email,
    client_first_name = u.first_name,
    client_last_name = u.last_name,
    client_phone_number = u.phone_number
FROM users u
WHERE b.client_id = u.id;

ALTER TABLE bookings
    ALTER COLUMN client_id DROP NOT NULL;

ALTER TABLE bookings
    DROP CONSTRAINT fk_bookings_client;

ALTER TABLE bookings
    ADD CONSTRAINT fk_bookings_client
        FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE SET NULL;
