package com.pricetracker.app.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for returning product information.
 */
public record ProductResponse(
    Long productId,
    
    String productUrl,
    
    String name,
    
    String imageUrl,
    
    BigDecimal lastCheckedPrice,
    
    Instant createdAt,
    
    Instant updatedAt
) {} 