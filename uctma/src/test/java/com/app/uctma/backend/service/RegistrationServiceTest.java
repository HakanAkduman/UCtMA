package com.app.uctma.backend.service;

import com.app.uctma.backend.dto.RegistrationReqDTO;
import com.app.uctma.backend.dto.VerifyEmailReqDTO;
import com.app.uctma.backend.model.AuthEmailVerification;
import com.app.uctma.backend.model.Role;
import com.app.uctma.backend.model.User;
import com.app.uctma.backend.repository.AuthEmailVerificationRepository;
import com.app.uctma.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationServiceTest {

    private UserRepository userRepository;
    private AuthEmailVerificationRepository verificationRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private PasswordPolicyService passwordPolicyService;
    private AuthAccountSettingsService settingsService;
    private RegistrationService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        verificationRepository = mock(AuthEmailVerificationRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        emailService = mock(EmailService.class);
        passwordPolicyService = mock(PasswordPolicyService.class);
        settingsService = mock(AuthAccountSettingsService.class);
        service = new RegistrationService(
                userRepository,
                verificationRepository,
                passwordEncoder,
                emailService,
                passwordPolicyService,
                settingsService
        );
    }

    @Test
    void createsPendingUserAndSendsVerificationCodeWithoutForcedPasswordChange() {
        when(settingsService.isRegistrationEnabled()).thenReturn(true);
        when(settingsService.isEmailVerificationRequired()).thenReturn(true);
        when(settingsService.isSettingEnabled(AuthAccountSettingsService.EMAIL_VERIFICATION_TTL_MINUTES))
                .thenReturn(true);
        when(settingsService.getQuantity(AuthAccountSettingsService.EMAIL_VERIFICATION_TTL_MINUTES)).thenReturn(15);
        when(settingsService.isSettingEnabled(AuthAccountSettingsService.EMAIL_VERIFICATION_RESEND_COOLDOWN_SECONDS))
                .thenReturn(true);
        when(settingsService.getQuantity(
                AuthAccountSettingsService.EMAIL_VERIFICATION_RESEND_COOLDOWN_SECONDS
        )).thenReturn(60);
        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("A-long-password1!")).thenReturn("encoded-password");
        when(passwordEncoder.encode(org.mockito.ArgumentMatchers.matches("\\d{6}"))).thenReturn("encoded-code");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(17L);
            return user;
        });

        var result = service.register(new RegistrationReqDTO(" NEW@EXAMPLE.COM ", "A-long-password1!"));

        assertTrue(result.message().contains("Doğrulama"));
        assertTrue(result.emailVerificationRequired());
        var userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User created = userCaptor.getValue();
        assertEquals("new@example.com", created.getEmail());
        assertEquals(Role.ROLE_USER, created.getRole());
        assertFalse(created.isEmailVerified());
        assertFalse(created.isNewPasswordFlag());
        verify(passwordPolicyService).validatePassword("A-long-password1!");
        verify(emailService).sendEmailVerificationCode(any(String.class), any(String.class), org.mockito.ArgumentMatchers.eq(15));
    }

    @Test
    void rejectsRegistrationWhenPublicSignUpIsDisabled() {
        when(settingsService.isRegistrationEnabled()).thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> service.register(new RegistrationReqDTO("new@example.com", "A-long-password1!")));

        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendEmailVerificationCode(any(), any(), any(Integer.class));
    }

    @Test
    void returnsTheVerificationModeActuallyUsedByTheBackend() {
        when(settingsService.isRegistrationEnabled()).thenReturn(true);
        when(settingsService.isEmailVerificationRequired()).thenReturn(false);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("A-long-password1!")).thenReturn("encoded-password");

        var result = service.register(new RegistrationReqDTO("user@example.com", "A-long-password1!"));

        assertFalse(result.emailVerificationRequired());
        assertTrue(result.message().contains("oluşturuldu"));
        var userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertTrue(userCaptor.getValue().isEmailVerified());
        verify(emailService, never()).sendEmailVerificationCode(any(), any(), any(Integer.class));
    }

    @Test
    void verifiesEmailAndRemovesOneTimeCode() {
        User user = User.builder().id(17L).email("new@example.com").role(Role.ROLE_USER)
                .emailVerified(false).newPasswordFlag(false).build();
        AuthEmailVerification verification = new AuthEmailVerification();
        verification.setUser(user);
        verification.setCodeHash("hashed-code");
        verification.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
        verification.setResendAvailableAt(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(60));
        verification.setFailedAttempts(0);
        when(userRepository.findByEmailIgnoreCaseForUpdate("new@example.com")).thenReturn(Optional.of(user));
        when(verificationRepository.findByUserIdForUpdate(17L)).thenReturn(Optional.of(verification));
        when(settingsService.isSettingEnabled(AuthAccountSettingsService.EMAIL_VERIFICATION_MAX_ATTEMPTS))
                .thenReturn(true);
        when(settingsService.getQuantity(AuthAccountSettingsService.EMAIL_VERIFICATION_MAX_ATTEMPTS)).thenReturn(5);
        when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(true);

        String result = service.verifyEmail(new VerifyEmailReqDTO("new@example.com", "123456"));

        assertTrue(result.contains("doğrulandı"));
        assertTrue(user.isEmailVerified());
        verify(userRepository).save(user);
        verify(verificationRepository).delete(verification);
    }

    @Test
    void resendDuringCooldownUsesTheSameGenericResponseAndDoesNotReplaceTheCode() {
        User user = User.builder().id(17L).email("new@example.com").role(Role.ROLE_USER)
                .emailVerified(false).build();
        AuthEmailVerification verification = new AuthEmailVerification();
        verification.setUser(user);
        verification.setResendAvailableAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1));
        when(userRepository.findByEmailIgnoreCaseForUpdate("new@example.com")).thenReturn(Optional.of(user));
        when(verificationRepository.findByUserId(17L)).thenReturn(Optional.of(verification));

        String response = service.resendVerificationCode("NEW@example.com");

        assertTrue(response.contains("Hesap doğrulamaya uygunsa"));
        verify(verificationRepository, never()).save(any(AuthEmailVerification.class));
        verify(emailService, never()).sendEmailVerificationCode(any(), any(), any(Integer.class));
    }
}
