package com.app.uctma.backend.config;

import com.app.uctma.backend.model.Role;
import com.app.uctma.backend.model.User;
import com.app.uctma.backend.repository.UserRepository;
import com.app.uctma.backend.service.PasswordPolicyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final String adminEmail;
    private final String adminPassword;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyService passwordPolicyService,
            @Value("${BOOTSTRAP_ADMIN_EMAIL:}") String adminEmail,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyService = passwordPolicyService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminEmail) && !StringUtils.hasText(adminPassword)) {
            logger.info("Admin bootstrap skipped because no bootstrap credentials are configured.");
            return;
        }
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            throw new IllegalStateException("Both BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD must be configured.");
        }
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            logger.info("Bootstrap admin already exists; leaving the existing account unchanged.");
            return;
        }

        passwordPolicyService.validatePassword(adminPassword);
        User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ROLE_ADMIN)
                .newPasswordFlag(true)
                .build();
        userRepository.save(admin);
        logger.info("Bootstrap admin account created. Change its password after the first login.");
    }
}
