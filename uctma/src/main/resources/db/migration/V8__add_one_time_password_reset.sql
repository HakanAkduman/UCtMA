ALTER TABLE users
    ADD COLUMN password_reset_hash VARCHAR(100),
    ADD COLUMN password_reset_expires_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN password_reset_requested_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN password_reset_session_id VARCHAR(36);
