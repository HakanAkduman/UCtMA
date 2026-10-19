CREATE TABLE auth_password_policies (
    id BIGSERIAL PRIMARY KEY,
    password_policy_code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    qty INTEGER NOT NULL DEFAULT 1 CHECK (qty >= 0)
);

INSERT INTO auth_password_policies (password_policy_code, description, enabled, qty) VALUES
    ('MIN_LENGTH', 'En az karakter sayısı', TRUE, 8),
    ('MAX_LENGTH', 'En fazla karakter sayısı', FALSE, 128),
    ('MIN_DIGITS', 'En az rakam sayısı', FALSE, 1),
    ('MIN_SPECIAL_CHARACTERS', 'En az özel karakter sayısı', FALSE, 1),
    ('MIN_LOWERCASE', 'En az küçük harf sayısı', FALSE, 1),
    ('MIN_UPPERCASE', 'En az büyük harf sayısı', FALSE, 1)
ON CONFLICT (password_policy_code) DO NOTHING;
