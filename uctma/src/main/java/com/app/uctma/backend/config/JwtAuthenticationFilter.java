package com.app.uctma.backend.config;

import com.app.uctma.backend.model.User;
import com.app.uctma.backend.service.JwtService;
import com.app.uctma.backend.service.PasswordPolicyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService; // Spring Security kullanıcı detayı servisi
    private final PasswordPolicyService passwordPolicyService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "/uctma/api/file/callback".equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Header'da Bearer token var mı kontrol et
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Token'ı başındaki "Bearer " kelimesinden ayır
        jwt = authHeader.substring(7);
        userEmail = jwtService.extractUsername(jwt);

        // 3. Kullanıcı email'i alındıysa ve oturumu henüz açılmadıysa doğrulama yap
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            if (!userDetails.isEnabled()) {
                writeForbidden(response, request, "Hesabınız devre dışı bırakılmış. Yöneticiyle iletişime geçin.");
                return;
            }

            if (jwtService.isTokenValid(jwt, userDetails)) {
                if (userDetails instanceof User user) {
                    String resetSessionId = jwtService.extractPasswordResetSessionId(jwt);
                    if (!user.isEmailVerified()) {
                        writeForbidden(response, request, "Devam etmek için e-posta adresinizi doğrulamalısınız.");
                        return;
                    }
                    if (user.getPasswordResetSessionId() != null) {
                        boolean resetSessionValid = user.getPasswordResetSessionId().equals(resetSessionId)
                                && user.getPasswordResetExpiresAt() != null
                                && LocalDateTime.now(ZoneOffset.UTC).isBefore(user.getPasswordResetExpiresAt());
                        if (!resetSessionValid || !isPasswordResetChangeRequest(request)) {
                            writeForbidden(response, request,
                                    "Parolanızı sıfırlamak için yeni parolanızı belirlemelisiniz.");
                            return;
                        }
                    } else if (resetSessionId != null) {
                        writeForbidden(response, request, "Parola sıfırlama oturumu geçersiz veya süresi dolmuş.");
                        return;
                    }
                    boolean expired = passwordPolicyService.isPasswordExpired(user.getPasswordChangedAt());
                    boolean passwordChangeRequired = user.isNewPasswordFlag() || expired;
                    if (passwordChangeRequired && !isAllowedDuringPasswordChange(request)) {
                        String message = user.isNewPasswordFlag()
                                ? "Devam etmek için yeni bir şifre belirlemelisiniz."
                                : "Şifrenizin geçerlilik süresi dolmuş; devam etmek için değiştirin.";
                        writeForbidden(response, request, message);
                        return;
                    }
                }
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 4. Kullanıcıyı SecurityContext'e koy!
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAllowedDuringPasswordChange(HttpServletRequest request) {
        String path = request.getServletPath();
        return (HttpMethod.POST.matches(request.getMethod()) && "/uctma/api/auth/change-password".equals(path))
                || (HttpMethod.GET.matches(request.getMethod())
                && "/uctma/api/auth/password-policies".equals(path));
    }

    private boolean isPasswordResetChangeRequest(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && "/uctma/api/auth/change-password".equals(request.getServletPath());
    }

    private void writeForbidden(HttpServletResponse response, HttpServletRequest request, String message)
            throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "success", false,
                "code", HttpStatus.FORBIDDEN.value(),
                "error", Map.of(
                        "errorMessage", message,
                        "path", request.getRequestURI(),
                        "timestamp", LocalDateTime.now().toString()
                )
        ));
    }
}
