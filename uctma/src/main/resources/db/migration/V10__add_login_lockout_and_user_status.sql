ALTER TABLE users
    ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_login_attempts >= 0);

INSERT INTO auth_account_settings (setting_code, description, enabled, qty)
VALUES ('LOGIN_FAILURE_LOCKOUT', 'Yanlış giriş sınırında kullanıcı hesabını devre dışı bırak', TRUE, 5)
ON CONFLICT (setting_code) DO NOTHING;
