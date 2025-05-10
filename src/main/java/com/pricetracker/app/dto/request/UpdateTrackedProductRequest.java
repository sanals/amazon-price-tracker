package com.pricetracker.app.dto.request;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/**
 * Request DTO for updating a tracked product.
 */
public record UpdateTrackedProductRequest(
    @DecimalMin(value = "0.01", message = "Desired price must be greater than zero")
    BigDecimal desiredPrice,
    
    Boolean notificationEnabled
) {} 