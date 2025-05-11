package com.pricetracker.app.scraping;

import org.jsoup.nodes.Document;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Strategy interface for website-specific scraping implementations.
 */
public interface ScraperStrategy {
    
    /**
     * Check if this strategy can handle the given URL.
     * 
     * @param url the product URL
     * @return true if this strategy can handle the URL, false otherwise
     */
    boolean canHandle(String url);
    
    /**
     * Extract the price from a product page.
     * 
     * @param doc the Jsoup document
     * @return an Optional containing the price if found, empty otherwise
     */
    Optional<BigDecimal> extractPrice(Document doc);
    
    /**
     * Extract the product name from a product page.
     * 
     * @param doc the Jsoup document
     * @return an Optional containing the product name if found, empty otherwise
     */
    Optional<String> extractName(Document doc);
    
    /**
     * Extract the product image URL from a product page.
     * 
     * @param doc the Jsoup document
     * @return an Optional containing the image URL if found, empty otherwise
     */
    Optional<String> extractImageUrl(Document doc);
} 