package com.pricetracker.app.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * JWT Authentication Filter
 * 
 * Intercepts all HTTP requests and validates JWT tokens in the Authorization
 * header.
 * If a valid token is found, the user is authenticated in the security context.
 * 
 * This filter only processes access tokens (JWTs), not refresh tokens.
 * Refresh tokens are handled separately by the AuthService.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    
    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    /**
     * Processes each request to validate JWT token if present
     * 
     * @param request     The HTTP request
     * @param response    The HTTP response
     * @param filterChain The filter chain
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        // Extract Authorization header
        final String authHeader = request.getHeader(AUTH_HEADER);
        // Skip if no Authorization header or not a Bearer token
        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract the JWT token (remove "Bearer " prefix)
        final String jwt = authHeader.substring(TOKEN_PREFIX.length());
        // Extract username from token
        final String username = jwtService.extractUsername(jwt);

        // If username exists and no authentication exists in context
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Load user details - this will be our User entity directly since we updated UserDetailsServiceImpl
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            // Validate token
            if (jwtService.isTokenValid(jwt, userDetails)) {
                // Create authentication token with user details and authorities
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,  // This will be our User entity since UserDetailsServiceImpl returns User
                        null,
                        userDetails.getAuthorities());
                // Set details from request
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // Update security context with authentication
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}