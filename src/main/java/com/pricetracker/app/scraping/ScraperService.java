package com.pricetracker.app.scraping;

import org.jsoup.nodes.Document;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for scraping product information from retail websites.
 */
public interface ScraperService {
    
    /**
     * Scrape the price from a product URL.
     * 
     * @param productUrl the product URL to scrape
     * @return an Optional containing the scraped price as BigDecimal, or empty if scraping failed
     */
    Optional<BigDecimal> scrapePrice(String productUrl);
    
    /**
     * Scrape detailed product information from a product URL.
     * 
     * @param productUrl the product URL to scrape
     * @return an Optional containing ProductDetails with name, image URL, and price, or empty if scraping failed
     */
    Optional<ProductDetails> scrapeProductDetails(String productUrl);
    
    /**
     * Register a new scraper strategy.
     * 
     * @param strategy the ScraperStrategy to register
     */
    void registerStrategy(ScraperStrategy strategy);
    
    /**
     * Fetch a document from a URL with proper error handling.
     * 
     * @param url the URL to fetch
     * @return the parsed JSoup Document
     * @throws IOException if the document cannot be fetched
     */
    Document fetchDocument(String url) throws IOException;
    
    /**
     * Find the appropriate scraper strategy for a given URL.
     * 
     * @param url the URL to find a strategy for
     * @return an Optional containing the appropriate ScraperStrategy, or empty if no suitable strategy found
     */
    Optional<ScraperStrategy> findStrategyForUrl(String url);
    
    /**
     * Get all registered scraper strategies.
     *
     * @return a list of all registered scraper strategies
     */
    List<ScraperStrategy> getStrategies();
    
    /**
     * Expand a shortened URL to its full form.
     * 
     * @param shortenedUrl the shortened URL to expand
     * @return the expanded URL, or the original URL if expansion failed
     */
    String expandShortenedUrl(String shortenedUrl);
} 