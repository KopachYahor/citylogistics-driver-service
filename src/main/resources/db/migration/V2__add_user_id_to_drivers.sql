ALTER TABLE drivers
    ADD COLUMN user_id BIGINT;

CREATE UNIQUE INDEX idx_drivers_user_id ON drivers(user_id);