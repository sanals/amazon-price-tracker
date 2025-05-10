package com.pricetracker.app.dto.response;

/**
 * DTO for returning basic user information.
 */
public record UserResponse(
    Long id,
    String username,
    String email
) {} 