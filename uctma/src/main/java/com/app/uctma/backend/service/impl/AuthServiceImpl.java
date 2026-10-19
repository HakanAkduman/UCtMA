package com.app.uctma.backend.service.impl;

import com.app.uctma.backend.dto.*;
import com.app.uctma.backend.model.User;
import com.app.uctma.backend.repository.UserRepository;
import com.app.uctma.backend.service.AuthService;
import com.app.uctma.backend.service.AuthAccountSettingsService;
import com.app.uctma.backend.service.EmailService;
import com.app.uctma.backend.service.JwtService;
import com.app.uctma.backend.service.PasswordPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final PasswordPolicyService passwordPolicyService;
    private final AuthAccountSettingsService accountSettingsService;
    private static final int RESET_PASSWORD_VALIDITY_MINUTES = 30;
    private static final int RESET_PASSWORD_COOLDOWN_SECONDS = 60;
    private static final String GENERIC_RESET_RESPONSE =
            "E-posta adresi sistemde kayıtlıysa parola sıfırlama bilgileri gönderildi.";

    @Override
    public String invite(InviteReqDTO inviteReqDTO) {

        String email = inviteReqDTO.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new RuntimeException("Bu email adresi zaten kullanımda!");
        }

        String tempPassword = UUID.randomUUID().toString().substring(0, 8);

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(tempPassword))
                .role(inviteReqDTO.getRole())
                .newPasswordFlag(true)
                .build();

        userRepository.save(user);

        emailService.sendTempPasswordEmail(user.getEmail(), tempPassword);

        return "Kullanıcı davet edildi. Geçici şifre e-posta adresine gönderildi.";
    }

    @Override
    public String delete(String email) {
        // İstek atan aktif kullanıcının email'ini al
        String currentUserEmail = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();

        if (currentUserEmail.equalsIgnoreCase(email)) {
            throw new RuntimeException("Kendi hesabınızı silemezsiniz!");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + email));

        userRepository.delete(user);
        return "Kullanıcı başarıyla silindi!";
    }

    @Override
    public List<UserListResDTO> listOfUsers() {
        return userRepository.findAll().stream().map(
                this::toUserListRes
        ).toList();
    }

    @Override
    @Transactional
    public UserListResDTO updateUserEnabled(String email, boolean enabled) {
        String currentUserEmail = Objects.requireNonNull(
                SecurityContextHolder.getContext().getAuthentication()
        ).getName();
        if (currentUserEmail.equalsIgnoreCase(email) && !enabled) {
            throw new IllegalArgumentException("Kendi hesabınızı devre dışı bırakamazsınız.");
        }

        User user = userRepository.findByEmailIgnoreCaseForUpdate(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı: " + email));
        user.setEnabled(enabled);
        if (enabled) {
            user.setFailedLoginAttempts(0);
        }
        return toUserListRes(userRepository.save(user));
    }

    @Override
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public LoginResDTO login(LoginReqDTO loginReqDTO) {

        String email = loginReqDTO.getEmail().trim().toLowerCase(Locale.ROOT);
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCaseForUpdate(email);
        if (!optionalUser.isPresent()) {
            throw new RuntimeException("Bu email adresine kayıtlı kullanıcı bulunamadı!");
        }
        User user= optionalUser.get();
        if (!user.isEnabled()) {
            throw new IllegalStateException("Hesabınız devre dışı bırakılmış. Yöneticiyle iletişime geçin.");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (user.getPasswordResetSessionId() != null) {
            if (user.getPasswordResetExpiresAt() != null && !now.isBefore(user.getPasswordResetExpiresAt())) {
                clearPasswordReset(user);
                userRepository.save(user);
            } else {
                throw new IllegalStateException("Parola sıfırlama oturumunda yeni parolanızı belirleyin.");
            }
        }

        boolean regularPasswordMatches = passwordEncoder.matches(loginReqDTO.getPassword(), user.getPassword());
        boolean resetPasswordMatches = user.getPasswordResetHash() != null
                && user.getPasswordResetExpiresAt() != null
                && now.isBefore(user.getPasswordResetExpiresAt())
                && passwordEncoder.matches(loginReqDTO.getPassword(), user.getPasswordResetHash());
        if (!regularPasswordMatches && !resetPasswordMatches) {
            if (user.getFailedLoginAttempts() < Integer.MAX_VALUE) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            }
            boolean lockoutEnabled = accountSettingsService.isSettingEnabled(
                    AuthAccountSettingsService.LOGIN_FAILURE_LOCKOUT
            );
            int maxAttempts = lockoutEnabled
                    ? accountSettingsService.getQuantity(AuthAccountSettingsService.LOGIN_FAILURE_LOCKOUT)
                    : 0;
            if (lockoutEnabled && user.getFailedLoginAttempts() >= maxAttempts) {
                user.setEnabled(false);
                userRepository.save(user);
                throw new IllegalArgumentException(
                        "Çok sayıda hatalı giriş nedeniyle hesabınız devre dışı bırakıldı. Yöneticiyle iletişime geçin."
                );
            }
            userRepository.save(user);
            if (lockoutEnabled) {
                int remainingAttempts = maxAttempts - user.getFailedLoginAttempts();
                throw new IllegalArgumentException(
                        "Şifreniz hatalı. Hesabınızın devre dışı kalmasına " + remainingAttempts + " deneme kaldı."
                );
            }
            throw new IllegalArgumentException("Şifreniz hatalı!");
        }
        if (!user.isEmailVerified()) {
            throw new IllegalStateException("E-posta adresiniz doğrulanmamış. E-posta doğrulama kodunu girin.");
        }

        boolean shouldPersistLoginState = false;
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            shouldPersistLoginState = true;
        }

        if (resetPasswordMatches && !regularPasswordMatches) {
            String resetSessionId = UUID.randomUUID().toString();
            user.setPasswordResetHash(null);
            user.setPasswordResetExpiresAt(now.plusMinutes(15));
            user.setPasswordResetSessionId(resetSessionId);
            user.setNewPasswordFlag(true);
            shouldPersistLoginState = true;
            userRepository.save(user);
            String token = jwtService.generatePasswordResetToken(
                    user.getEmail(),
                    user.getRole().name(),
                    resetSessionId
            );
            return new LoginResDTO(user.getEmail(), token, "Bearer", true, user.getRole(), true);
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        boolean passwordExpired = passwordPolicyService.isPasswordExpired(user.getPasswordChangedAt());
        if (passwordExpired && !user.isNewPasswordFlag()) {
            user.setNewPasswordFlag(true);
            shouldPersistLoginState = true;
        }
        if (shouldPersistLoginState) {
            userRepository.save(user);
        }

        return new LoginResDTO(
                user.getEmail(),
                token,
                "Bearer",
                user.isNewPasswordFlag() || passwordExpired,
                user.getRole(),
                false
        );
    }

    @Override
    @Transactional
    public String forgotPassword(ForgotPasswordReqDTO request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCaseForUpdate(email)
                .filter(User::isEmailVerified)
                .ifPresent(user -> {
                    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                    if (user.getPasswordResetRequestedAt() != null
                            && now.isBefore(user.getPasswordResetRequestedAt()
                            .plusSeconds(RESET_PASSWORD_COOLDOWN_SECONDS))) {
                        return;
                    }

                    String temporaryPassword = UUID.randomUUID().toString().replace("-", "").substring(0,12);
                    user.setPasswordResetHash(passwordEncoder.encode(temporaryPassword));
                    user.setPasswordResetExpiresAt(now.plusMinutes(RESET_PASSWORD_VALIDITY_MINUTES));
                    user.setPasswordResetRequestedAt(now);
                    user.setPasswordResetSessionId(null);
                    userRepository.save(user);
                    emailService.sendPasswordResetTemporaryPassword(
                            user.getEmail(),
                            temporaryPassword,
                            RESET_PASSWORD_VALIDITY_MINUTES
                    );
                });
        return GENERIC_RESET_RESPONSE;
    }

    @Override
    @Transactional
    public String changePassword(ChangePasswordReqDTO changePasswordReqDTO, String passwordResetSessionId) {
        if (!changePasswordReqDTO.getNewPassword().equals(changePasswordReqDTO.getConfirmNewPassword())) {
            throw new RuntimeException("Yeni şifreler birbiriyle uyuşmuyor!");
        }


        String currentUserEmail = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        boolean resettingWithOneTimePassword = user.getPasswordResetSessionId() != null
                && user.getPasswordResetSessionId().equals(passwordResetSessionId)
                && user.getPasswordResetExpiresAt() != null
                && LocalDateTime.now(ZoneOffset.UTC).isBefore(user.getPasswordResetExpiresAt());
        if (user.getPasswordResetSessionId() != null && !resettingWithOneTimePassword) {
            throw new IllegalStateException("Parola sıfırlama oturumu geçersiz veya süresi dolmuş.");
        }
        boolean currentPasswordRequired = !resettingWithOneTimePassword && !user.isNewPasswordFlag();
        if (currentPasswordRequired
                && (changePasswordReqDTO.getCurrentPassword() == null
                || !passwordEncoder.matches(changePasswordReqDTO.getCurrentPassword(), user.getPassword()))) {
            throw new RuntimeException("Mevcut şifreniz hatalı!");
        }

        passwordPolicyService.validatePassword(changePasswordReqDTO.getNewPassword());

        user.setPassword(passwordEncoder.encode(changePasswordReqDTO.getNewPassword()));
        user.setNewPasswordFlag(false);
        user.setPasswordChangedAt(LocalDateTime.now(ZoneOffset.UTC));
        clearPasswordReset(user);

        userRepository.save(user);
        return currentUserEmail;
    }

    private void clearPasswordReset(User user) {
        user.setPasswordResetHash(null);
        user.setPasswordResetExpiresAt(null);
        user.setPasswordResetSessionId(null);
    }

    private UserListResDTO toUserListRes(User user) {
        return new UserListResDTO(
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getFailedLoginAttempts()
        );
    }

}
