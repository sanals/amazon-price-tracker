package com.pricetracker.app.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for returning tracked product information.
 */
public record TrackedProductResponse(
    Long id,
    
    ProductResponse product,
    
    BigDecimal desiredPrice,
    
    Boolean notificationEnabled,
    
    Integer checkIntervalMinutes,
    
    Instant createdAt,
    
    Instant updatedAt
) {} 