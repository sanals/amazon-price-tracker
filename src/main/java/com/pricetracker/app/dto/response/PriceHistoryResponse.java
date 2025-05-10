package com.pricetracker.app.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for returning price history information.
 */
public record PriceHistoryResponse(
    Long id,
    
    BigDecimal price,
    
    Instant timestamp
) {} 