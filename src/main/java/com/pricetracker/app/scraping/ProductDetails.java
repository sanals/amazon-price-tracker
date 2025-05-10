package com.pricetracker.app.scraping;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * DTO for storing scraped product details.
 */
public record ProductDetails(
    Optional<String> name,
    Optional<String> imageUrl,
    Optional<BigDecimal> price
) {
    /**
     * Create a new ProductDetails instance with all fields empty.
     */
    public static ProductDetails empty() {
        return new ProductDetails(
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }
} 