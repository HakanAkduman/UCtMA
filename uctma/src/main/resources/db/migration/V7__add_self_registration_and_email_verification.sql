ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE auth_account_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    qty INTEGER NOT NULL DEFAULT 1 CHECK (qty >= 0)
);

INSERT INTO auth_account_settings (setting_code, description, enabled, qty) VALUES
    ('SELF_REGISTRATION', 'Kullanıcıların kendi hesaplarını oluşturabilmesi', FALSE, 0),
    ('EMAIL_VERIFICATION_REQUIRED', 'Self-registration için e-posta doğrulaması', TRUE, 0),
    ('EMAIL_VERIFICATION_TTL_MINUTES', 'Doğrulama kodunun geçerlilik süresi (dakika)', TRUE, 15),
    ('EMAIL_VERIFICATION_RESEND_COOLDOWN_SECONDS', 'Doğrulama kodu yeniden gönderim bekleme süresi (saniye)', TRUE, 60),
    ('EMAIL_VERIFICATION_MAX_ATTEMPTS', 'Doğrulama kodu deneme limiti', TRUE, 5)
ON CONFLICT (setting_code) DO NOTHING;

CREATE TABLE auth_email_verifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    code_hash VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    resend_available_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0)
);
