package com.app.uctma.backend.service;

import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    String generateToken(String username, String role);
    String extractUsername(String token);
    boolean isTokenValid(String token, UserDetails userDetails);
}
