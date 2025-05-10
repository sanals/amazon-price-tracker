package com.pricetracker.app.exception;

/**
 * Exception thrown when a user attempts to track a product they are already tracking.
 */
public class ProductAlreadyTrackedException extends RuntimeException {
    
    public ProductAlreadyTrackedException(String message) {
        super(message);
    }
    
    public ProductAlreadyTrackedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProductAlreadyTrackedException(Long userId, String productUrl) {
        super(String.format("User with ID %d is already tracking product with URL: %s", userId, productUrl));
    }
} 