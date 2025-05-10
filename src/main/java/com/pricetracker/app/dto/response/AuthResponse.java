package com.pricetracker.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authentication Response DTO
 * 
 * Contains authentication details returned after successful login,
 * including JWT access token, refresh token, and user information.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String username;
    private String role;
} 