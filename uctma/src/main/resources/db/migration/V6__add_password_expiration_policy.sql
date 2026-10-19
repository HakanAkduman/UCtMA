ALTER TABLE users
    ADD COLUMN password_changed_at TIMESTAMP WITHOUT TIME ZONE;

UPDATE users
SET password_changed_at = timezone('UTC', CURRENT_TIMESTAMP)
WHERE password_changed_at IS NULL;

ALTER TABLE users
    ALTER COLUMN password_changed_at SET DEFAULT timezone('UTC', CURRENT_TIMESTAMP),
    ALTER COLUMN password_changed_at SET NOT NULL;

INSERT INTO auth_password_policies (password_policy_code, description, enabled, qty)
VALUES ('MAX_AGE_DAYS', 'Şifrenin geçerli olacağı azami gün sayısı', FALSE, 90)
ON CONFLICT (password_policy_code) DO NOTHING;
