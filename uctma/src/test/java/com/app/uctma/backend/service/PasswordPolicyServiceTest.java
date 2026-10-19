package com.app.uctma.backend.service;

import com.app.uctma.backend.dto.PasswordPolicyUpdateItemDTO;
import com.app.uctma.backend.dto.PasswordPolicyUpdateReqDTO;
import com.app.uctma.backend.model.AuthPasswordPolicy;
import com.app.uctma.backend.repository.AuthPasswordPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordPolicyServiceTest {

    private AuthPasswordPolicyRepository repository;
    private PasswordPolicyService service;
    private List<AuthPasswordPolicy> policies;

    @BeforeEach
    void setUp() {
        repository = mock(AuthPasswordPolicyRepository.class);
        service = new PasswordPolicyService(repository);
        policies = List.of(
                policy("MIN_LENGTH", true, 8),
                policy("MAX_LENGTH", true, 12),
                policy("MIN_DIGITS", true, 2),
                policy("MIN_SPECIAL_CHARACTERS", true, 1),
                policy("MIN_LOWERCASE", true, 1),
                policy("MIN_UPPERCASE", true, 1),
                policy("MAX_AGE_DAYS", false, 90)
        );
        when(repository.findAllByOrderByIdAsc()).thenReturn(policies);
    }

    @Test
    void validatesConfiguredLengthAndCharacterTypeRequirements() {
        assertDoesNotThrow(() -> service.validatePassword("Aa11!!bc"));

        var shortPassword = assertThrows(IllegalArgumentException.class,
                () -> service.validatePassword("Aa1!bc"));
        org.junit.jupiter.api.Assertions.assertTrue(shortPassword.getMessage().contains("en az 8 karakter"));

        var exception = assertThrows(IllegalArgumentException.class,
                () -> service.validatePassword("password"));
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("rakam"));
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("büyük harf"));
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("özel karakter"));
    }

    @Test
    void countsUnicodeCodePointsForLength() {
        assertDoesNotThrow(() -> service.validatePassword("A😀11!bcd"));
        assertThrows(IllegalArgumentException.class, () -> service.validatePassword("A😀11!bc"));
    }

    @Test
    void reportsExpiredPasswordWhenMaximumAgePolicyIsEnabled() {
        policies.stream()
                .filter(policy -> policy.getPasswordPolicyCode().equals("MAX_AGE_DAYS"))
                .findFirst()
                .orElseThrow()
                .setEnabled(true);
        policies.stream()
                .filter(policy -> policy.getPasswordPolicyCode().equals("MAX_AGE_DAYS"))
                .findFirst()
                .orElseThrow()
                .setQty(30);

        org.junit.jupiter.api.Assertions.assertTrue(
                service.isPasswordExpired(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusDays(31))
        );
        org.junit.jupiter.api.Assertions.assertFalse(
                service.isPasswordExpired(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusDays(29))
        );
    }

    @Test
    void rejectsInconsistentAdminPolicySettings() {
        PasswordPolicyUpdateReqDTO request = new PasswordPolicyUpdateReqDTO(List.of(
                update("MIN_LENGTH", true, 20),
                update("MAX_LENGTH", true, 12),
                update("MIN_DIGITS", true, 1),
                update("MIN_SPECIAL_CHARACTERS", false, 0),
                update("MIN_LOWERCASE", false, 0),
                update("MIN_UPPERCASE", false, 0),
                update("MAX_AGE_DAYS", false, 90)
        ));

        assertThrows(IllegalArgumentException.class, () -> service.updatePolicies(request));
        verify(repository, never()).saveAll(anyList());
    }

    @Test
    void savesAdminPolicyChangesWhenConfigurationIsConsistent() {
        PasswordPolicyUpdateReqDTO request = new PasswordPolicyUpdateReqDTO(List.of(
                update("MIN_LENGTH", true, 10),
                update("MAX_LENGTH", true, 20),
                update("MIN_DIGITS", true, 2),
                update("MIN_SPECIAL_CHARACTERS", false, 1),
                update("MIN_LOWERCASE", false, 1),
                update("MIN_UPPERCASE", false, 1),
                update("MAX_AGE_DAYS", true, 90)
        ));
        when(repository.saveAll(policies)).thenReturn(policies);

        service.updatePolicies(request);

        verify(repository).saveAll(policies);
    }

    private AuthPasswordPolicy policy(String code, boolean enabled, int qty) {
        AuthPasswordPolicy policy = new AuthPasswordPolicy();
        policy.setPasswordPolicyCode(code);
        policy.setDescription(code);
        policy.setEnabled(enabled);
        policy.setQty(qty);
        return policy;
    }

    private PasswordPolicyUpdateItemDTO update(String code, boolean enabled, int qty) {
        return new PasswordPolicyUpdateItemDTO(code, enabled, qty);
    }
}
