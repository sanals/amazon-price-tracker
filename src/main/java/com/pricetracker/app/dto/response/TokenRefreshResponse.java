package com.pricetracker.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token Refresh Response DTO
 * 
 * Contains the new access token and refresh token information
 * returned after a successful token refresh operation.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenRefreshResponse {
    private String accessToken;
    private String refreshToken;
    @Builder.Default
    private String tokenType = "Bearer";
} 