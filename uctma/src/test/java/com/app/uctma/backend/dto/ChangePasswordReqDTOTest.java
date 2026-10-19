package com.app.uctma.backend.dto;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChangePasswordReqDTOTest {

    @Test
    void requiresAtLeastEightCharactersForNewPassword() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var validator = validatorFactory.getValidator();
            var request = new ChangePasswordReqDTO();
            request.setCurrentPassword("current-password");
            request.setConfirmNewPassword("1234567");
            request.setNewPassword("1234567");

            var violations = validator.validate(request);
            assertEquals(1, violations.size());
            assertTrue(violations.stream().anyMatch(violation ->
                    violation.getPropertyPath().toString().equals("newPassword")));

            request.setNewPassword("12345678");
            request.setConfirmNewPassword("12345678");
            assertTrue(validator.validate(request).isEmpty());
        }
    }
}
