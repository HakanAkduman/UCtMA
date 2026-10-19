UPDATE auth_account_settings
SET setting_code = 'REGISTRATION_ENABLED',
    description = 'Herkese açık kullanıcı kaydı'
WHERE setting_code = 'SELF_REGISTRATION';

UPDATE auth_account_settings
SET description = 'Kayıt için e-posta doğrulaması'
WHERE setting_code = 'EMAIL_VERIFICATION_REQUIRED';
