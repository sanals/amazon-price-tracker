package com.pricetracker.app.scraping;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service interface for scraping product information from web pages.
 */
public interface ScraperService {
    
    /**
     * Scrape the current price from a product URL.
     * 
     * @param productUrl the URL of the product page
     * @return an Optional containing the price if found, empty otherwise
     */
    Optional<BigDecimal> scrapePrice(String productUrl);
    
    /**
     * Scrape product details (name, image URL, price) from a product URL.
     * 
     * @param productUrl the URL of the product page
     * @return an Optional containing the product details if found, empty otherwise
     */
    Optional<ProductDetails> scrapeProductDetails(String productUrl);
} 