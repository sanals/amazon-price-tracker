package com.pricetracker.app.scraping;

import org.jsoup.nodes.Document;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Interface defining a scraping strategy for a specific website.
 * All website-specific scraping implementations should implement this interface.
 */
public interface ScraperStrategy {
    
    /**
     * Determines if this strategy can handle the given URL.
     * 
     * @param url the product URL to check
     * @return true if this strategy can handle the URL, false otherwise
     */
    boolean canHandle(String url);
    
    /**
     * Extract the price from the given document.
     * 
     * @param doc the JSoup document to extract from
     * @return an Optional containing the extracted price as BigDecimal, or empty if extraction failed
     */
    Optional<BigDecimal> extractPrice(Document doc);
    
    /**
     * Extract the product name from the given document.
     * 
     * @param doc the JSoup document to extract from
     * @return an Optional containing the extracted name, or empty if extraction failed
     */
    Optional<String> extractName(Document doc);
    
    /**
     * Extract the product image URL from the given document.
     * 
     * @param doc the JSoup document to extract from
     * @return an Optional containing the extracted image URL, or empty if extraction failed
     */
    Optional<String> extractImageUrl(Document doc);
    
    /**
     * Scrape the complete product details from a document.
     * 
     * @param doc the JSoup document to extract from
     * @return an Optional containing ProductDetails with all extracted information
     */
    default Optional<ProductDetails> scrapeProductDetails(Document doc) {
        Optional<String> name = extractName(doc);
        Optional<String> imageUrl = extractImageUrl(doc);
        Optional<BigDecimal> price = extractPrice(doc);
        
        // If we couldn't extract any of the essential details, return empty
        if (name.isEmpty() && imageUrl.isEmpty() && price.isEmpty()) {
            return Optional.empty();
        }
        
        return Optional.of(new ProductDetails(name, imageUrl, price));
    }
    
    /**
     * Check if a document is a CAPTCHA verification page.
     * 
     * @param doc the JSoup document to check
     * @return true if the document is a CAPTCHA page, false otherwise
     */
    boolean isCaptchaPage(Document doc);
    
    /**
     * Expand a shortened URL if the strategy supports it.
     * 
     * @param shortenedUrl the shortened URL to expand
     * @return the expanded URL, or the original URL if expansion failed or isn't supported
     */
    default String expandShortenedUrl(String shortenedUrl) {
        return shortenedUrl; // Default implementation returns the original URL
    }
} 