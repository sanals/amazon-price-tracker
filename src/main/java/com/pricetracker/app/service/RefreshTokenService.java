package com.pricetracker.app.service;

import com.pricetracker.app.entity.RefreshToken;
import java.util.Optional;

public interface RefreshTokenService {
    /**
     * Creates a new refresh token for the specified user
     * @param username The username associated with the token
     * @return The created refresh token entity
     */
    RefreshToken createRefreshToken(String username);
    
    /**
     * Verifies that a refresh token is not expired
     * @param token The token to verify
     * @return The verified token if valid
     */
    RefreshToken verifyExpiration(RefreshToken token);
    
    /**
     * Finds a refresh token by its token string
     * @param token The token string to find
     * @return Optional containing the token if found
     */
    Optional<RefreshToken> findByToken(String token);
    
    /**
     * Deletes all refresh tokens for a user
     * @param userId The user ID whose tokens should be deleted
     */
    void deleteByUserId(Long userId);
    
    /**
     * Validates a refresh token string
     * @param token The token string to validate
     * @return The username extracted from the token if valid
     */
    String validateRefreshToken(String token);
    
    /**
     * Determines if the refresh token is stored in database or is JWT-based
     * @return true if using database storage, false if using JWT
     */
    boolean isUsingDatabaseStorage();
} 