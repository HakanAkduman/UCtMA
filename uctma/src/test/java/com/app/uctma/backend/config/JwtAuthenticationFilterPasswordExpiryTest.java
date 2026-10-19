package com.app.uctma.backend.config;

import com.app.uctma.backend.model.Role;
import com.app.uctma.backend.model.User;
import com.app.uctma.backend.service.JwtService;
import com.app.uctma.backend.service.PasswordPolicyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterPasswordExpiryTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksExpiredPasswordFromProtectedRoutes() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        User user = user();
        when(jwtService.extractUsername("token")).thenReturn(user.getEmail());
        when(jwtService.isTokenValid("token", user)).thenReturn(true);
        when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
        when(passwordPolicyService.isPasswordExpired(user.getPasswordChangedAt())).thenReturn(true);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtService,
                userDetailsService,
                passwordPolicyService,
                new ObjectMapper()
        );
        MockHttpServletRequest request = request("GET", "/uctma/api/file");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        var json = new ObjectMapper().readTree(response.getContentAsString());
        assertEquals(false, json.get("success").asBoolean());
        assertEquals(403, json.get("code").asInt());
        org.junit.jupiter.api.Assertions.assertTrue(
                json.get("error").get("errorMessage").asText().contains("geçerlilik süresi dolmuş")
        );
        assertEquals("/uctma/api/file", json.get("error").get("path").asText());
        org.junit.jupiter.api.Assertions.assertTrue(json.get("error").has("timestamp"));
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void allowsExpiredUserToReachPasswordChangeEndpoint() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        User user = user();
        when(jwtService.extractUsername("token")).thenReturn(user.getEmail());
        when(jwtService.isTokenValid("token", user)).thenReturn(true);
        when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
        when(passwordPolicyService.isPasswordExpired(user.getPasswordChangedAt())).thenReturn(true);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtService,
                userDetailsService,
                passwordPolicyService,
                new ObjectMapper()
        );
        MockHttpServletRequest request = request("POST", "/uctma/api/auth/change-password");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void allowsOneTimeResetSessionOnlyToChangePassword() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        User user = user();
        user.setNewPasswordFlag(true);
        user.setPasswordResetSessionId("reset-session");
        user.setPasswordResetExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
        when(jwtService.extractUsername("token")).thenReturn(user.getEmail());
        when(jwtService.extractPasswordResetSessionId("token")).thenReturn("reset-session");
        when(jwtService.isTokenValid("token", user)).thenReturn(true);
        when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtService,
                userDetailsService,
                passwordPolicyService,
                new ObjectMapper()
        );
        MockHttpServletRequest request = request("POST", "/uctma/api/auth/change-password");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void blocksOneTimeResetSessionFromAllOtherRoutes() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        User user = user();
        user.setPasswordResetSessionId("reset-session");
        user.setPasswordResetExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
        when(jwtService.extractUsername("token")).thenReturn(user.getEmail());
        when(jwtService.extractPasswordResetSessionId("token")).thenReturn("reset-session");
        when(jwtService.isTokenValid("token", user)).thenReturn(true);
        when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtService,
                userDetailsService,
                passwordPolicyService,
                new ObjectMapper()
        );
        MockHttpServletRequest request = request("GET", "/uctma/api/file");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void blocksDisabledUserFromUsingAnExistingToken() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        PasswordPolicyService passwordPolicyService = mock(PasswordPolicyService.class);
        User user = user();
        user.setEnabled(false);
        when(jwtService.extractUsername("token")).thenReturn(user.getEmail());
        when(userDetailsService.loadUserByUsername(user.getEmail())).thenReturn(user);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                jwtService,
                userDetailsService,
                passwordPolicyService,
                new ObjectMapper()
        );
        MockHttpServletRequest request = request("GET", "/uctma/api/file");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        verify(jwtService, never()).isTokenValid("token", user);
        verify(chain, never()).doFilter(request, response);
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.addHeader("Authorization", "Bearer token");
        return request;
    }

    private User user() {
        return User.builder()
                .email("user@example.com")
                .password("encoded")
                .role(Role.ROLE_USER)
                .newPasswordFlag(false)
                .passwordChangedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(100))
                .build();
    }
}
