package com.app.uctma.backend.service;

import com.app.uctma.backend.dto.RegistrationReqDTO;
import com.app.uctma.backend.dto.RegistrationResDTO;
import com.app.uctma.backend.dto.VerifyEmailReqDTO;
import com.app.uctma.backend.model.AuthEmailVerification;
import com.app.uctma.backend.model.Role;
import com.app.uctma.backend.model.User;
import com.app.uctma.backend.repository.AuthEmailVerificationRepository;
import com.app.uctma.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String GENERIC_RESEND_MESSAGE =
            "Hesap doğrulamaya uygunsa yeni bir kod e-posta adresine gönderildi.";

    private final UserRepository userRepository;
    private final AuthEmailVerificationRepository verificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PasswordPolicyService passwordPolicyService;
    private final AuthAccountSettingsService accountSettingsService;

    @Transactional
    public RegistrationResDTO register(RegistrationReqDTO request) {
        if (!accountSettingsService.isRegistrationEnabled()) {
            throw new IllegalStateException("Kullanıcı kaydı şu anda kapalı.");
        }
        passwordPolicyService.validatePassword(request.password());

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("Bu e-posta adresiyle kayıt oluşturulamıyor.");
        }

        boolean verificationRequired = accountSettingsService.isEmailVerificationRequired();
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_USER)
                .newPasswordFlag(false)
                .emailVerified(!verificationRequired)
                .build();
        userRepository.save(user);

        if (verificationRequired) {
            issueVerificationCode(user, null);
            return new RegistrationResDTO("Doğrulama kodu e-posta adresinize gönderildi.", true);
        }
        return new RegistrationResDTO("Hesabınız oluşturuldu. Giriş yapabilirsiniz.", false);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public String verifyEmail(VerifyEmailReqDTO request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCaseForUpdate(email)
                .orElseThrow(() -> new IllegalArgumentException("Doğrulama kodu geçersiz veya süresi dolmuş."));
        if (user.isEmailVerified()) {
            return "E-posta adresiniz zaten doğrulanmış.";
        }

        AuthEmailVerification verification = verificationRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Doğrulama kodu geçersiz veya süresi dolmuş."));
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (!now.isBefore(verification.getExpiresAt())) {
            verificationRepository.delete(verification);
            throw new IllegalArgumentException("Doğrulama kodunun süresi dolmuş. Yeni kod isteyin.");
        }
        int maxAttempts = accountSettingsService.isSettingEnabled(
                AuthAccountSettingsService.EMAIL_VERIFICATION_MAX_ATTEMPTS
        ) ? accountSettingsService.getQuantity(AuthAccountSettingsService.EMAIL_VERIFICATION_MAX_ATTEMPTS) : 5;
        if (verification.getFailedAttempts() >= maxAttempts) {
            verificationRepository.delete(verification);
            throw new IllegalArgumentException("Doğrulama deneme sınırı aşıldı. Yeni kod isteyin.");
        }
        if (!passwordEncoder.matches(request.code(), verification.getCodeHash())) {
            verification.setFailedAttempts(verification.getFailedAttempts() + 1);
            verificationRepository.save(verification);
            throw new IllegalArgumentException("Doğrulama kodu geçersiz veya süresi dolmuş.");
        }

        user.setEmailVerified(true);
        userRepository.save(user);
        verificationRepository.delete(verification);
        return "E-posta adresiniz doğrulandı. Giriş yapabilirsiniz.";
    }

    @Transactional
    public String resendVerificationCode(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCaseForUpdate(email)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(user -> {
                    AuthEmailVerification existing = verificationRepository.findByUserId(user.getId()).orElse(null);
                    issueVerificationCode(user, existing);
                });
        return GENERIC_RESEND_MESSAGE;
    }

    private void issueVerificationCode(User user, AuthEmailVerification existing) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (existing != null && now.isBefore(existing.getResendAvailableAt())) {
            return;
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        int ttlMinutes = accountSettingsService.isSettingEnabled(
                AuthAccountSettingsService.EMAIL_VERIFICATION_TTL_MINUTES
        ) ? accountSettingsService.getQuantity(AuthAccountSettingsService.EMAIL_VERIFICATION_TTL_MINUTES) : 15;
        int cooldownSeconds = accountSettingsService.isSettingEnabled(
                AuthAccountSettingsService.EMAIL_VERIFICATION_RESEND_COOLDOWN_SECONDS
        ) ? accountSettingsService.getQuantity(
                AuthAccountSettingsService.EMAIL_VERIFICATION_RESEND_COOLDOWN_SECONDS
        ) : 60;

        AuthEmailVerification verification = existing == null ? new AuthEmailVerification() : existing;
        verification.setUser(user);
        verification.setCodeHash(passwordEncoder.encode(code));
        verification.setExpiresAt(now.plusMinutes(ttlMinutes));
        verification.setResendAvailableAt(now.plusSeconds(cooldownSeconds));
        verification.setFailedAttempts(0);
        verificationRepository.save(verification);
        emailService.sendEmailVerificationCode(user.getEmail(), code, ttlMinutes);
    }
}
