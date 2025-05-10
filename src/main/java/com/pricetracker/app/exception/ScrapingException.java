package com.pricetracker.app.exception;

/**
 * Exception thrown when there's an error during web scraping operations.
 */
public class ScrapingException extends RuntimeException {

    public ScrapingException(String message) {
        super(message);
    }

    public ScrapingException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScrapingException(String url, String operation, Throwable cause) {
        super(String.format("Failed to %s from URL: %s", operation, url), cause);
    }
} 