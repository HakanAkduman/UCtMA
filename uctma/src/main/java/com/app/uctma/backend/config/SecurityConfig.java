package com.app.uctma.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // REST API kullandığımız için CSRF kapalı
                .authorizeHttpRequests(auth -> auth
                        // 1. Herkese açık endpoint'ler (Giriş yapmadan erişilebilir)
                        .requestMatchers("/uctma/api/auth/login").permitAll()

                        // 2. Sadece ADMIN olanların erişebileceği endpoint'ler
                        .requestMatchers("/uctma/api/auth/register").hasAuthority("ROLE_ADMIN")

                        // 3. Giriş yapmış HER HANGİ bir kullanıcının erişebileceği endpoint'ler
                        .requestMatchers("/uctma/api/auth/change-password").authenticated()

                        // 4. Kalan tüm endpoint'ler için oturum açmış olma şartı
                        .anyRequest().authenticated()
                )
                // Stateless (Oturumsuz) mimari: Sunucuda session tutmuyoruz, doğrulama token üzerinden yapılıyor
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                // Bizim yazdığımız JWT Filtresini varsayılan kullanıcı adı/şifre filtresinden ÖNCE çalıştır
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
