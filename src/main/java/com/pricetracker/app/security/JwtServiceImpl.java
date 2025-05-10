package com.pricetracker.app.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Service for JWT token operations
 * 
 * Handles creating, validating, and extracting information from JWT tokens.
 * These JWT tokens are used as access tokens for API authorization.
 */
@Service
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:#{T(com.pricetracker.app.constants.AppConstants).DEFAULT_JWT_EXPIRATION}}")
    private long jwtExpiration;

    /**
     * Extracts the username from a JWT token
     * 
     * @param token JWT token
     * @return Username from token subject claim
     */
    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Alias for extractUsername for backward compatibility
     */
    @Override
    public String getUsernameFromToken(String token) {
        return extractUsername(token);
    }

    /**
     * Validates if a token is valid for a given user
     * 
     * @param token       JWT token to validate
     * @param userDetails User details to validate against
     * @return true if token is valid, false otherwise
     */
    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Generates a JWT token for a user
     * 
     * @param userDetails User details to include in token
     * @return JWT token string
     */
    @Override
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a JWT token with additional claims
     * 
     * @param extraClaims Additional claims to include in token
     * @param userDetails User details to include in token
     * @return JWT token string
     */
    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts
                .builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Checks if a token is expired
     * 
     * @param token JWT token
     * @return true if token is expired, false otherwise
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date from a token
     * 
     * @param token JWT token
     * @return Expiration date
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from a token
     * 
     * @param token          JWT token
     * @param claimsResolver Function to extract specific claim
     * @return Extracted claim value
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from a token
     * 
     * @param token JWT token
     * @return All claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Gets the signing key for JWT verification
     * 
     * @return Signing key
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}