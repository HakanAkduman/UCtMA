package com.app.uctma.backend.service.impl;

import com.app.uctma.backend.dto.LoginReqDTO;
import com.app.uctma.backend.dto.ForgotPasswordReqDTO;
import com.app.uctma.backend.model.Role;
import com.app.uctma.backend.model.User;
import com.app.uctma.backend.repository.UserRepository;
import com.app.uctma.backend.service.EmailService;
import com.app.uctma.backend.service.JwtService;
import com.app.uctma.backend.service.AuthAccountSettingsService;
import com.app.uctma.backend.service.PasswordPolicyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void persistsPasswordChangeRequiredWhenLoginFindsExpiredPassword() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        EmailService emailService = mock(EmailService.class);
        JwtService jwtService = mock(JwtService.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        AuthAccountSettingsService accountSettingsService = mock(AuthAccountSettingsService.class);
        User user = User.builder()
                .email("user@example.com")
                .password("encoded-password")
                .role(Role.ROLE_USER)
                .newPasswordFlag(false)
                .failedLoginAttempts(3)
                .passwordChangedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(100))
                .build();
        when(userRepository.findByEmailIgnoreCaseForUpdate("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "encoded-password")).thenReturn(true);
        when(jwtService.generateToken("user@example.com", Role.ROLE_USER.name())).thenReturn("token");
        when(passwordPolicyService.isPasswordExpired(user.getPasswordChangedAt())).thenReturn(true);

        AuthServiceImpl authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                emailService,
                jwtService,
                passwordPolicyService,
                accountSettingsService
        );
        LoginReqDTO request = new LoginReqDTO();
        request.setEmail("user@example.com");
        request.setPassword("current-password");
        var result = authService.login(request);

        assertTrue(result.isNewPasswordRequired());
        org.junit.jupiter.api.Assertions.assertFalse(result.isPasswordResetSession());
        assertTrue(user.isNewPasswordFlag());
        assertEquals(0, user.getFailedLoginAttempts());
        verify(userRepository).save(user);
    }

    @Test
    void disablesAccountWhenConfiguredWrongPasswordLimitIsReached() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        EmailService emailService = mock(EmailService.class);
        JwtService jwtService = mock(JwtService.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        AuthAccountSettingsService accountSettingsService = mock(AuthAccountSettingsService.class);
        User user = User.builder()
                .email("user@example.com")
                .password("encoded-password")
                .role(Role.ROLE_USER)
                .failedLoginAttempts(1)
                .build();
        when(userRepository.findByEmailIgnoreCaseForUpdate("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);
        when(accountSettingsService.isSettingEnabled(AuthAccountSettingsService.LOGIN_FAILURE_LOCKOUT))
                .thenReturn(true);
        when(accountSettingsService.getQuantity(AuthAccountSettingsService.LOGIN_FAILURE_LOCKOUT)).thenReturn(2);

        AuthServiceImpl authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                emailService,
                jwtService,
                passwordPolicyService,
                accountSettingsService
        );
        LoginReqDTO request = new LoginReqDTO();
        request.setEmail("user@example.com");
        request.setPassword("wrong-password");

        var error = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertTrue(error.getMessage().contains("devre dışı bırakıldı"));
        assertEquals(2, user.getFailedLoginAttempts());
        org.junit.jupiter.api.Assertions.assertFalse(user.isEnabled());
        verify(userRepository).save(user);
    }

    @Test
    void rejectsLoginForDisabledAccountBeforeCheckingPassword() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        AuthAccountSettingsService accountSettingsService = mock(AuthAccountSettingsService.class);
        User user = User.builder()
                .email("user@example.com")
                .password("encoded-password")
                .role(Role.ROLE_USER)
                .enabled(false)
                .build();
        when(userRepository.findByEmailIgnoreCaseForUpdate("user@example.com")).thenReturn(Optional.of(user));
        AuthServiceImpl authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                mock(EmailService.class),
                mock(JwtService.class),
                mock(PasswordPolicyService.class),
                accountSettingsService
        );
        LoginReqDTO request = new LoginReqDTO();
        request.setEmail("user@example.com");
        request.setPassword("any-password");

        var error = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> authService.login(request)
        );

        assertTrue(error.getMessage().contains("devre dışı"));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void adminReenablesAccountAndResetsFailedLoginCounter() {
        UserRepository userRepository = mock(UserRepository.class);
        User user = User.builder()
                .email("user@example.com")
                .role(Role.ROLE_USER)
                .enabled(false)
                .failedLoginAttempts(5)
                .build();
        when(userRepository.findByEmailIgnoreCaseForUpdate("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", "credentials")
        );
        AuthServiceImpl authService = new AuthServiceImpl(
                userRepository,
                mock(PasswordEncoder.class),
                mock(EmailService.class),
                mock(JwtService.class),
                mock(PasswordPolicyService.class),
                mock(AuthAccountSettingsService.class)
        );

        var result = authService.updateUserEnabled("user@example.com", true);

        assertTrue(user.isEnabled());
        assertEquals(0, user.getFailedLoginAttempts());
        assertTrue(result.enabled());
        assertEquals(0, result.failedLoginAttempts());
        verify(userRepository).save(user);
    }

    @Test
    void preventsLoginUntilEmailIsVerified() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        EmailService emailService = mock(EmailService.class);
        JwtService jwtService = mock(JwtService.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        AuthAccountSettingsService accountSettingsService = mock(AuthAccountSettingsService.class);
        User user = User.builder()
                .email("user@example.com")
                .password("encoded-password")
                .role(Role.ROLE_USER)
                .emailVerified(false)
                .build();
        when(userRepository.findByEmailIgnoreCaseForUpdate("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "encoded-password")).thenReturn(true);

        AuthServiceImpl authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                emailService,
                jwtService,
                passwordPolicyService,
                accountSettingsService
        );
        LoginReqDTO request = new LoginReqDTO();
        request.setEmail("user@example.com");
        request.setPassword("current-password");

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> authService.login(request));
        org.mockito.Mockito.verify(jwtService, org.mockito.Mockito.never()).generateToken(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void sendsOneTimeResetPasswordWithoutRevealingWhetherEmailExists() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        EmailService emailService = mock(EmailService.class);
        JwtService jwtService = mock(JwtService.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        AuthAccountSettingsService accountSettingsService = mock(AuthAccountSettingsService.class);
        User user = User.builder()
                .email("user@example.com")
                .password("existing-password")
                .role(Role.ROLE_USER)
                .emailVerified(true)
                .build();
        when(userRepository.findByEmailIgnoreCaseForUpdate("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-reset-password");

        AuthServiceImpl authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                emailService,
                jwtService,
                passwordPolicyService,
                accountSettingsService
        );
        String response = authService.forgotPassword(new ForgotPasswordReqDTO(" USER@example.com "));

        assertTrue(response.contains("kayıtlıysa"));
        assertNotNull(user.getPasswordResetHash());
        assertNotNull(user.getPasswordResetExpiresAt());
        assertNotNull(user.getPasswordResetRequestedAt());
        verify(emailService).sendPasswordResetTemporaryPassword(
                eq("user@example.com"),
                anyString(),
                eq(30)
        );
        verify(userRepository).save(user);

        when(userRepository.findByEmailIgnoreCaseForUpdate("missing@example.com")).thenReturn(Optional.empty());
        String missingUserResponse = authService.forgotPassword(new ForgotPasswordReqDTO("missing@example.com"));
        assertEquals(response, missingUserResponse);
        verify(emailService, never()).sendPasswordResetTemporaryPassword(
                eq("missing@example.com"),
                anyString(),
                anyInt()
        );
    }

    @Test
    void consumesTemporaryPasswordOnLoginAndCreatesPasswordChangeOnlySession() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        EmailService emailService = mock(EmailService.class);
        JwtService jwtService = mock(JwtService.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        AuthAccountSettingsService accountSettingsService = mock(AuthAccountSettingsService.class);
        User user = User.builder()
                .email("user@example.com")
                .password("encoded-existing-password")
                .passwordResetHash("encoded-reset-password")
                .passwordResetExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10))
                .role(Role.ROLE_USER)
                .emailVerified(true)
                .build();
        when(userRepository.findByEmailIgnoreCaseForUpdate("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("temporary-password", "encoded-existing-password")).thenReturn(false);
        when(passwordEncoder.matches("temporary-password", "encoded-reset-password")).thenReturn(true);
        when(jwtService.generatePasswordResetToken(eq("user@example.com"), eq("ROLE_USER"), anyString()))
                .thenReturn("reset-session-token");

        AuthServiceImpl authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                emailService,
                jwtService,
                passwordPolicyService,
                accountSettingsService
        );
        LoginReqDTO request = new LoginReqDTO();
        request.setEmail("user@example.com");
        request.setPassword("temporary-password");

        var result = authService.login(request);

        assertTrue(result.isNewPasswordRequired());
        assertTrue(result.isPasswordResetSession());
        assertEquals("reset-session-token", result.getToken());
        org.junit.jupiter.api.Assertions.assertNull(user.getPasswordResetHash());
        assertNotNull(user.getPasswordResetSessionId());
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> authService.login(request)
        );
        verify(jwtService, never()).generateToken(anyString(), anyString());
        verify(userRepository).save(user);
    }
}
