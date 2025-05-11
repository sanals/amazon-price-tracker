package com.pricetracker.app.scraping;

/**
 * Custom exception for scraping-related errors.
 */
public class ScrapingException extends RuntimeException {
    
    public ScrapingException(String message) {
        super(message);
    }
    
    public ScrapingException(String message, Throwable cause) {
        super(message, cause);
    }
} 