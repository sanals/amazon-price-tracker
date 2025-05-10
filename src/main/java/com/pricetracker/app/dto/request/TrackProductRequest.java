package com.pricetracker.app.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * Request DTO for tracking a new product.
 */
public record TrackProductRequest(
    @NotBlank(message = "Product URL is required")
    @Pattern(regexp = "https?://.*", message = "Must be a valid URL starting with http:// or https://")
    String productUrl,
    
    @NotNull(message = "Desired price is required")
    @DecimalMin(value = "0.01", message = "Desired price must be greater than zero")
    BigDecimal desiredPrice
) {} 