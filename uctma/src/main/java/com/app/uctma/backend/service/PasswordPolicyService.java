package com.app.uctma.backend.service;

import com.app.uctma.backend.dto.PasswordPolicyDTO;
import com.app.uctma.backend.dto.PasswordPolicyUpdateItemDTO;
import com.app.uctma.backend.dto.PasswordPolicyUpdateReqDTO;
import com.app.uctma.backend.model.AuthPasswordPolicy;
import com.app.uctma.backend.repository.AuthPasswordPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PasswordPolicyService {

    private static final int MAX_POLICY_QTY = 512;
    private static final Set<String> SUPPORTED_CODES = Set.of(
            "MIN_LENGTH",
            "MAX_LENGTH",
            "MIN_DIGITS",
            "MIN_SPECIAL_CHARACTERS",
            "MIN_LOWERCASE",
            "MIN_UPPERCASE",
            "MAX_AGE_DAYS"
    );

    private final AuthPasswordPolicyRepository policyRepository;

    @Transactional(readOnly = true)
    public List<PasswordPolicyDTO> getPolicies() {
        return policyRepository.findAllByOrderByIdAsc().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public List<PasswordPolicyDTO> updatePolicies(PasswordPolicyUpdateReqDTO request) {
        List<AuthPasswordPolicy> policies = policyRepository.findAllByOrderByIdAsc();
        Map<String, AuthPasswordPolicy> policiesByCode = policies.stream()
                .collect(Collectors.toMap(AuthPasswordPolicy::getPasswordPolicyCode, Function.identity()));
        Map<String, PasswordPolicyUpdateItemDTO> updatesByCode = request.policies().stream()
                .collect(Collectors.toMap(
                        PasswordPolicyUpdateItemDTO::passwordPolicyCode,
                        Function.identity(),
                        (first, duplicate) -> {
                            throw new IllegalArgumentException("Aynı parola politikası birden fazla kez gönderilemez.");
                        }
                ));

        if (!updatesByCode.keySet().equals(policiesByCode.keySet())) {
            throw new IllegalArgumentException("Parola politikası listesi eksik veya geçersiz.");
        }
        if (!SUPPORTED_CODES.containsAll(policiesByCode.keySet())) {
            throw new IllegalStateException("Veritabanında desteklenmeyen parola politikası kodu bulunuyor.");
        }

        for (AuthPasswordPolicy policy : policies) {
            PasswordPolicyUpdateItemDTO update = updatesByCode.get(policy.getPasswordPolicyCode());
            if (update.qty() < 0 || update.qty() > MAX_POLICY_QTY) {
                throw new IllegalArgumentException("Politika miktarı 0 ile 512 arasında olmalıdır.");
            }
            if (update.enabled() && update.qty() < 1) {
                throw new IllegalArgumentException("Etkin parola politikalarının miktarı en az 1 olmalıdır.");
            }
            policy.setEnabled(update.enabled());
            policy.setQty(update.qty());
        }

        validateConfiguration(policies);
        return policyRepository.saveAll(policies).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public void validatePassword(String password) {
        List<AuthPasswordPolicy> policies = policyRepository.findAllByOrderByIdAsc();
        validateConfiguredPolicies(policies);
        int length = password.codePointCount(0, password.length());
        List<String> violations = new ArrayList<>();

        for (AuthPasswordPolicy policy : policies) {
            if (!policy.isEnabled()) {
                continue;
            }
            if (policy.getPasswordPolicyCode().equals("MAX_AGE_DAYS")) {
                continue;
            }
            int qty = policy.getQty();
            switch (policy.getPasswordPolicyCode()) {
                case "MIN_LENGTH" -> {
                    if (length < qty) violations.add("en az " + qty + " karakter");
                }
                case "MAX_LENGTH" -> {
                    if (length > qty) violations.add("en fazla " + qty + " karakter");
                }
                case "MIN_DIGITS" -> {
                    if (countMatching(password, Character::isDigit) < qty) {
                        violations.add("en az " + qty + " rakam");
                    }
                }
                case "MIN_SPECIAL_CHARACTERS" -> {
                    if (countMatching(password, codePoint -> !Character.isLetterOrDigit(codePoint)) < qty) {
                        violations.add("en az " + qty + " özel karakter");
                    }
                }
                case "MIN_LOWERCASE" -> {
                    if (countMatching(password, Character::isLowerCase) < qty) {
                        violations.add("en az " + qty + " küçük harf");
                    }
                }
                case "MIN_UPPERCASE" -> {
                    if (countMatching(password, Character::isUpperCase) < qty) {
                        violations.add("en az " + qty + " büyük harf");
                    }
                }
                default -> throw new IllegalStateException(
                        "Desteklenmeyen parola politikası: " + policy.getPasswordPolicyCode()
                );
            }
        }

        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Yeni şifre parola politikalarını karşılamıyor: "
                    + String.join(", ", violations) + ".");
        }
    }

    @Transactional(readOnly = true)
    public boolean isPasswordExpired(LocalDateTime passwordChangedAt) {
        List<AuthPasswordPolicy> policies = policyRepository.findAllByOrderByIdAsc();
        validateConfiguredPolicies(policies);
        return policies.stream()
                .filter(policy -> policy.getPasswordPolicyCode().equals("MAX_AGE_DAYS"))
                .filter(AuthPasswordPolicy::isEnabled)
                .anyMatch(policy -> passwordChangedAt == null
                        || !LocalDateTime.now(ZoneOffset.UTC).isBefore(passwordChangedAt.plusDays(policy.getQty())));
    }

    private long countMatching(String value, java.util.function.IntPredicate predicate) {
        return value.codePoints().filter(predicate).count();
    }

    private void validateConfiguration(List<AuthPasswordPolicy> policies) {
        validateConfiguredPolicies(policies);
        Map<String, AuthPasswordPolicy> byCode = policies.stream().collect(
                Collectors.toMap(AuthPasswordPolicy::getPasswordPolicyCode, Function.identity())
        );
        AuthPasswordPolicy minLength = byCode.get("MIN_LENGTH");
        AuthPasswordPolicy maxLength = byCode.get("MAX_LENGTH");
        if (minLength.isEnabled() && maxLength.isEnabled() && minLength.getQty() > maxLength.getQty()) {
            throw new IllegalArgumentException("Minimum karakter uzunluğu maksimum uzunluktan büyük olamaz.");
        }

        int requiredCharacterCount = policies.stream()
                .filter(AuthPasswordPolicy::isEnabled)
                .filter(policy -> Set.of("MIN_DIGITS", "MIN_SPECIAL_CHARACTERS", "MIN_LOWERCASE", "MIN_UPPERCASE")
                        .contains(policy.getPasswordPolicyCode()))
                .mapToInt(AuthPasswordPolicy::getQty)
                .sum();
        if (maxLength.isEnabled() && requiredCharacterCount > maxLength.getQty()) {
            throw new IllegalArgumentException(
                    "Karakter türü koşullarının toplamı maksimum şifre uzunluğunu aşamaz."
            );
        }
    }

    private void validateConfiguredPolicies(List<AuthPasswordPolicy> policies) {
        Set<String> configuredCodes = policies.stream()
                .map(AuthPasswordPolicy::getPasswordPolicyCode)
                .collect(Collectors.toSet());
        if (!configuredCodes.equals(SUPPORTED_CODES)) {
            throw new IllegalStateException("Parola politikası yapılandırması eksik veya desteklenmiyor.");
        }
    }

    private PasswordPolicyDTO toDTO(AuthPasswordPolicy policy) {
        return new PasswordPolicyDTO(
                policy.getId(),
                policy.getPasswordPolicyCode(),
                policy.getDescription(),
                policy.isEnabled(),
                policy.getQty()
        );
    }
}
