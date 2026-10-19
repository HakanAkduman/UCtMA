package com.app.uctma.backend.service;

import com.app.uctma.backend.dto.AuthAccountSettingDTO;
import com.app.uctma.backend.dto.AuthAccountSettingUpdateItemDTO;
import com.app.uctma.backend.dto.AuthAccountSettingsUpdateReqDTO;
import com.app.uctma.backend.dto.AuthPublicSettingsDTO;
import com.app.uctma.backend.model.AuthAccountSetting;
import com.app.uctma.backend.repository.AuthAccountSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthAccountSettingsService {

    public static final String REGISTRATION_ENABLED = "REGISTRATION_ENABLED";
    public static final String EMAIL_VERIFICATION_REQUIRED = "EMAIL_VERIFICATION_REQUIRED";
    public static final String EMAIL_VERIFICATION_TTL_MINUTES = "EMAIL_VERIFICATION_TTL_MINUTES";
    public static final String EMAIL_VERIFICATION_RESEND_COOLDOWN_SECONDS =
            "EMAIL_VERIFICATION_RESEND_COOLDOWN_SECONDS";
    public static final String EMAIL_VERIFICATION_MAX_ATTEMPTS = "EMAIL_VERIFICATION_MAX_ATTEMPTS";
    public static final String LOGIN_FAILURE_LOCKOUT = "LOGIN_FAILURE_LOCKOUT";

    private static final Set<String> SUPPORTED_CODES = Set.of(
            REGISTRATION_ENABLED,
            EMAIL_VERIFICATION_REQUIRED,
            EMAIL_VERIFICATION_TTL_MINUTES,
            EMAIL_VERIFICATION_RESEND_COOLDOWN_SECONDS,
            EMAIL_VERIFICATION_MAX_ATTEMPTS,
            LOGIN_FAILURE_LOCKOUT
    );

    private final AuthAccountSettingRepository settingRepository;

    @Transactional(readOnly = true)
    public List<AuthAccountSettingDTO> getSettings() {
        return settingRepository.findAllByOrderByIdAsc().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public List<AuthAccountSettingDTO> updateSettings(AuthAccountSettingsUpdateReqDTO request) {
        List<AuthAccountSetting> settings = settingRepository.findAllByOrderByIdAsc();
        Map<String, AuthAccountSetting> settingsByCode = settings.stream()
                .collect(Collectors.toMap(AuthAccountSetting::getSettingCode, Function.identity()));
        Map<String, AuthAccountSettingUpdateItemDTO> updatesByCode = request.settings().stream()
                .collect(Collectors.toMap(
                        AuthAccountSettingUpdateItemDTO::settingCode,
                        Function.identity(),
                        (first, duplicate) -> {
                            throw new IllegalArgumentException("Aynı auth ayarı birden fazla kez gönderilemez.");
                        }
                ));

        if (!SUPPORTED_CODES.containsAll(settingsByCode.keySet())
                || !updatesByCode.keySet().equals(settingsByCode.keySet())
                || !settingsByCode.keySet().equals(SUPPORTED_CODES)) {
            throw new IllegalStateException("Auth ayarları listesi eksik veya desteklenmeyen değer içeriyor.");
        }

        for (AuthAccountSetting setting : settings) {
            AuthAccountSettingUpdateItemDTO update = updatesByCode.get(setting.getSettingCode());
            validateValue(update);
            setting.setEnabled(update.enabled());
            setting.setQty(update.qty());
        }
        return settingRepository.saveAll(settings).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public AuthPublicSettingsDTO getPublicSettings() {
        Map<String, AuthAccountSetting> settings = loadSettingsByCode();
        return new AuthPublicSettingsDTO(
                settings.get(REGISTRATION_ENABLED).isEnabled(),
                settings.get(EMAIL_VERIFICATION_REQUIRED).isEnabled()
        );
    }

    @Transactional(readOnly = true)
    public boolean isRegistrationEnabled() {
        return setting(REGISTRATION_ENABLED).isEnabled();
    }

    @Transactional(readOnly = true)
    public boolean isEmailVerificationRequired() {
        return setting(EMAIL_VERIFICATION_REQUIRED).isEnabled();
    }

    @Transactional(readOnly = true)
    public boolean isSettingEnabled(String settingCode) {
        return setting(settingCode).isEnabled();
    }

    @Transactional(readOnly = true)
    public int getQuantity(String settingCode) {
        return setting(settingCode).getQty();
    }

    private AuthAccountSetting setting(String code) {
        return settingRepository.findBySettingCode(code)
                .orElseThrow(() -> new IllegalStateException("Auth ayarı bulunamadı: " + code));
    }

    private Map<String, AuthAccountSetting> loadSettingsByCode() {
        List<AuthAccountSetting> settings = settingRepository.findAllByOrderByIdAsc();
        Map<String, AuthAccountSetting> byCode = settings.stream()
                .collect(Collectors.toMap(AuthAccountSetting::getSettingCode, Function.identity()));
        if (!byCode.keySet().equals(SUPPORTED_CODES)) {
            throw new IllegalStateException("Auth ayarları eksik veya desteklenmiyor.");
        }
        return byCode;
    }

    private void validateValue(AuthAccountSettingUpdateItemDTO update) {
        if (update.qty() < 0 || update.qty() > 10080) {
            throw new IllegalArgumentException("Auth ayar miktarı 0 ile 10080 arasında olmalıdır.");
        }
        if (switch (update.settingCode()) {
            case EMAIL_VERIFICATION_TTL_MINUTES -> update.enabled() && (update.qty() < 1 || update.qty() > 1440);
            case EMAIL_VERIFICATION_RESEND_COOLDOWN_SECONDS ->
                    update.enabled() && (update.qty() < 1 || update.qty() > 3600);
            case EMAIL_VERIFICATION_MAX_ATTEMPTS -> update.enabled() && (update.qty() < 1 || update.qty() > 20);
            case LOGIN_FAILURE_LOCKOUT -> update.enabled() && (update.qty() < 1 || update.qty() > 20);
            default -> false;
        }) {
            throw new IllegalArgumentException("E-posta doğrulama ayarları tanımlı güvenli sınırlar içinde olmalıdır.");
        }
    }

    private AuthAccountSettingDTO toDTO(AuthAccountSetting setting) {
        return new AuthAccountSettingDTO(
                setting.getId(),
                setting.getSettingCode(),
                setting.getDescription(),
                setting.isEnabled(),
                setting.getQty()
        );
    }
}
