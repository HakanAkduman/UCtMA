package com.app.uctma.backend.config;

import com.app.uctma.backend.model.Role;
import com.app.uctma.backend.model.User;
import com.app.uctma.backend.repository.UserRepository;
import com.app.uctma.backend.service.PasswordPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private PasswordPolicyService passwordPolicyService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        passwordPolicyService = mock(PasswordPolicyService.class);
    }

    @Test
    void createsAdminWithEncodedPasswordAndForcedPasswordChange() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("a-secure-initial-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bootstrap("admin@example.com", "a-secure-initial-password").run(arguments());

        var savedUser = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("admin@example.com", savedUser.getValue().getEmail());
        assertEquals("encoded-password", savedUser.getValue().getPassword());
        assertEquals(Role.ROLE_ADMIN, savedUser.getValue().getRole());
        assertTrue(savedUser.getValue().isNewPasswordFlag());
        verify(passwordEncoder).encode("a-secure-initial-password");
        verify(passwordPolicyService).validatePassword("a-secure-initial-password");
    }

    @Test
    void leavesExistingAccountUnchanged() {
        User existingUser = User.builder().email("admin@example.com").role(Role.ROLE_USER).build();
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(existingUser));

        bootstrap("admin@example.com", "a-secure-initial-password").run(arguments());

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
        verify(passwordPolicyService, never()).validatePassword(any());
        assertEquals(Role.ROLE_USER, existingUser.getRole());
    }

    @Test
    void skipsWhenNoBootstrapCredentialsAreConfigured() {
        bootstrap("", "").run(arguments());

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void rejectsPartialBootstrapCredentials() {
        assertThrows(IllegalStateException.class, () -> bootstrap("admin@example.com", "").run(arguments()));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void acceptsEightCharacterBootstrapPassword() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("12345678")).thenReturn("encoded-password");

        bootstrap("admin@example.com", "12345678").run(arguments());

        verify(userRepository).save(any(User.class));
        verify(passwordPolicyService).validatePassword("12345678");
    }

    private AdminBootstrap bootstrap(String email, String password) {
        return new AdminBootstrap(userRepository, passwordEncoder, passwordPolicyService, email, password);
    }

    private DefaultApplicationArguments arguments() {
        return new DefaultApplicationArguments(new String[0]);
    }
}
