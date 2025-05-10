package com.pricetracker.app.service.impl;

import com.pricetracker.app.entity.RefreshToken;
import com.pricetracker.app.entity.User;
import com.pricetracker.app.exception.TokenRefreshException;
import com.pricetracker.app.repository.RefreshTokenRepository;
import com.pricetracker.app.repository.UserRepository;
import com.pricetracker.app.service.RefreshTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation for handling refresh tokens.
 * Supports two storage strategies:
 * 1. Database storage (default) - Refresh tokens stored in database table
 * 2. JWT-based (stateless) - Refresh tokens are signed JWTs, not stored in database
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${jwt.refresh-token.expiration}")
    private Long refreshTokenDuration;
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.refresh-token.storage:database}")
    private String refreshTokenStorage;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    /**
     * Creates a new refresh token for a user
     * Based on configured storage strategy:
     * - For database storage: Generates UUID token and stores in database
     * - For JWT storage: Generates signed JWT token (not stored in database)
     */
    @Override
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        // For database storage, delete existing tokens first
        if (isUsingDatabaseStorage()) {
            // Delete existing refresh tokens for this user
            refreshTokenRepository.deleteByUser(user);
            
            RefreshToken refreshToken = RefreshToken.builder()
                    .user(user)
                    .token(UUID.randomUUID().toString())
                    .expiryDate(Instant.now().plusMillis(refreshTokenDuration))
                    .build();

            return refreshTokenRepository.save(refreshToken);
        } else {
            // For JWT-based refresh tokens, generate a JWT without storing in DB
            String jwtRefreshToken = generateJwtRefreshToken(user);
            
            // Create a transient RefreshToken object (not persisted)
            return RefreshToken.builder()
                    .user(user)
                    .token(jwtRefreshToken)
                    .expiryDate(Instant.now().plusMillis(refreshTokenDuration))
                    .build();
        }
    }

    /**
     * Generates a JWT-based refresh token
     * Only used when refreshTokenStorage is set to 'jwt'
     */
    private String generateJwtRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenDuration))
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Gets the signing key for JWT tokens
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Verifies a refresh token is not expired
     * For database tokens: Checks expiry date in database record
     * For JWT tokens: The JWT validation handles expiration
     */
    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (isUsingDatabaseStorage()) {
            if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
                refreshTokenRepository.delete(token);
                throw new TokenRefreshException(token.getToken(), "Refresh token was expired. Please make a new signin request");
            }
            return token;
        } else {
            // For JWT refresh tokens, verification happens in validateRefreshToken method
            // This should only be called after validation, so we just return the token
            return token;
        }
    }

    /**
     * Finds a refresh token by its token string
     * For database tokens: Looks up in the database
     * For JWT tokens: Validates JWT and returns a transient RefreshToken if valid
     */
    @Override
    public Optional<RefreshToken> findByToken(String token) {
        if (isUsingDatabaseStorage()) {
            return refreshTokenRepository.findByToken(token);
        } else {
            try {
                // For JWT tokens, validate and create a transient token object
                String username = validateRefreshToken(token);
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new TokenRefreshException(token, "User not found for refresh token"));
                
                RefreshToken refreshToken = RefreshToken.builder()
                        .user(user)
                        .token(token)
                        // Set expiry date from JWT claims
                        .expiryDate(extractExpiryFromJwt(token))
                        .build();
                
                return Optional.of(refreshToken);
            } catch (Exception e) {
                log.error("Error validating JWT refresh token", e);
                return Optional.empty();
            }
        }
    }
    
    /**
     * Extracts expiry date from JWT token
     */
    private Instant extractExpiryFromJwt(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.getExpiration().toInstant();
    }

    /**
     * Validates a JWT refresh token
     * @param token The token to validate
     * @return The username from the token if valid
     */
    @Override
    public String validateRefreshToken(String token) {
        try {
            if (isUsingDatabaseStorage()) {
                // For database storage, token validation happens differently
                Optional<RefreshToken> storedToken = refreshTokenRepository.findByToken(token);
                if (storedToken.isEmpty()) {
                    throw new TokenRefreshException(token, "Refresh token not found in database");
                }
                verifyExpiration(storedToken.get());
                return storedToken.get().getUser().getUsername();
            } else {
                // For JWT tokens, parse and validate
                Claims claims = Jwts.parser()
                        .verifyWith(getSigningKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
                
                return claims.getSubject();
            }
        } catch (ExpiredJwtException e) {
            throw new TokenRefreshException(token, "Refresh token expired");
        } catch (UnsupportedJwtException e) {
            throw new TokenRefreshException(token, "Unsupported JWT token");
        } catch (MalformedJwtException e) {
            throw new TokenRefreshException(token, "Invalid JWT token");
        } catch (SignatureException e) {
            throw new TokenRefreshException(token, "Invalid JWT signature");
        } catch (IllegalArgumentException e) {
            throw new TokenRefreshException(token, "JWT claims string is empty");
        }
    }

    /**
     * Deletes all refresh tokens for a user
     * For database tokens: Deletes from database
     * For JWT tokens: No-op (tokens are stateless)
     */
    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        if (isUsingDatabaseStorage()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            refreshTokenRepository.deleteByUser(user);
        }
        // For JWT-based tokens, there's nothing to delete as they're not stored in DB
        // Client-side would still need to discard the token
    }
    
    /**
     * Determines if the application is using database storage for refresh tokens
     * @return true if using database storage, false if using JWT
     */
    @Override
    public boolean isUsingDatabaseStorage() {
        return "database".equalsIgnoreCase(refreshTokenStorage);
    }
} 