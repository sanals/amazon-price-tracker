package com.pricetracker.app.scraping;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base implementation of ScraperStrategy with common functionality.
 * Website-specific scrapers can extend this class to reuse common methods.
 */
public abstract class BaseScraperStrategy implements ScraperStrategy {
    
    protected static final Logger log = LoggerFactory.getLogger(BaseScraperStrategy.class);
    
    // Max length for price text to avoid related products sections 
    protected static final int MAX_PRICE_TEXT_LENGTH = 100;
    
    // Common pattern for price extraction (handles various currency formats)
    protected static final Pattern COMMON_PRICE_PATTERN = 
            Pattern.compile("(?:[₹$€£¥]|Rs\\.?|USD|EUR|GBP|INR)?\\s*([\\d,]+(?:\\.\\d+)?)");
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCaptchaPage(Document doc) {
        // Check for common CAPTCHA page elements
        boolean hasCaptchaTitle = !doc.select("title:contains(Robot Check), title:contains(CAPTCHA), title:contains(Enter the characters)").isEmpty();
        boolean hasCaptchaImage = !doc.select("img[src*=captcha], form[action*=validateCaptcha]").isEmpty();
        boolean hasCaptchaText = !doc.select("h4:contains(Enter the characters), p:contains(not a robot), h4:contains(Type the characters)").isEmpty();
        boolean hasVerificationForm = !doc.select("form[action*=verify]").isEmpty();
        
        boolean result = hasCaptchaTitle || hasCaptchaImage || hasCaptchaText || hasVerificationForm;
        
        if (result) {
            log.warn("Detected CAPTCHA page with following signals: title={}, image={}, text={}, form={}", 
                    hasCaptchaTitle, hasCaptchaImage, hasCaptchaText, hasVerificationForm);
        }
        
        return result;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String expandShortenedUrl(String shortenedUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URI(shortenedUrl).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            connection.setRequestMethod("HEAD");
            
            int responseCode = connection.getResponseCode();
            if (responseCode >= 300 && responseCode < 400) {
                String expandedUrl = connection.getHeaderField("Location");
                if (expandedUrl != null && !expandedUrl.isEmpty()) {
                    log.debug("Expanded URL {} to {}", shortenedUrl, expandedUrl);
                    // If still a relative URL, handle it
                    if (expandedUrl.startsWith("/")) {
                        expandedUrl = url.getProtocol() + "://" + url.getHost() + expandedUrl;
                    }
                    return expandedUrl;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to expand shortened URL {}: {}", shortenedUrl, e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return shortenedUrl; // Return original if expansion fails
    }
    
    /**
     * Helper method to parse a price from text using the common price pattern.
     * 
     * @param text the text to parse
     * @return an Optional containing the parsed price, or empty if parsing failed
     */
    protected Optional<BigDecimal> parsePrice(String text) {
        if (text == null || text.isEmpty()) {
            return Optional.empty();
        }
        
        // Skip excessively long text
        if (text.length() > MAX_PRICE_TEXT_LENGTH) {
            log.debug("Skipping oversized price text in parsePrice (length {})", text.length());
            return Optional.empty();
        }
        
        try {
            // More aggressive approach to clean the text with byte-level examination
            String cleanedText = text;
            
            // Handle the specific corrupted Rupee symbol pattern
            if (text.contains("Γé╣")) {
                // Replace the corrupted pattern with standard rupee symbol or Rs.
                cleanedText = text.replace("Γé╣", "Rs.");
                log.debug("Replaced corrupted Rupee symbol in text");
            }
            
            log.debug("Original price text: '{}', Cleaned text: '{}'", text, cleanedText);
            
            // Try to extract numeric part directly with a more aggressive approach
            // Look for patterns like digits with commas and decimal points
            Matcher digitMatcher = Pattern.compile("(\\d{1,3}(,\\d{3})*(\\.\\d+)?)").matcher(cleanedText);
            if (digitMatcher.find()) {
                String priceStr = digitMatcher.group(1).replace(",", "");
                log.debug("Extracted price digits: '{}'", priceStr);
                return Optional.of(new BigDecimal(priceStr));
            }
            
            // Fallback to the regular pattern matcher if direct extraction fails
            Matcher matcher = COMMON_PRICE_PATTERN.matcher(cleanedText);
            if (matcher.find()) {
                String priceStr = matcher.group(1).replace(",", ""); // Remove commas
                return Optional.of(new BigDecimal(priceStr));
            }
        } catch (Exception e) {
            log.debug("Failed to parse price from text: '{}' - Error: {}", text, e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Helper method to extract price using a list of selectors.
     * 
     * @param doc the document to extract from
     * @param selectors array of CSS selectors to try
     * @return Optional containing the price if found, empty otherwise
     */
    protected Optional<BigDecimal> extractPriceWithSelectors(Document doc, String[] selectors) {
        for (String selector : selectors) {
            try {
                Element element = doc.selectFirst(selector);
                if (element != null) {
                    String priceText = element.text().trim();
                    
                    // Skip excessively long text
                    if (priceText.length() > MAX_PRICE_TEXT_LENGTH) {
                        log.debug("Skipping oversized price text for selector {} (length {})", 
                                selector, priceText.length());
                        continue;
                    }
                    
                    log.debug("Found potential price element with selector {}: '{}'", 
                            selector, priceText);
                    
                    Optional<BigDecimal> price = parsePrice(priceText);
                    if (price.isPresent()) {
                        return price;
                    }
                }
            } catch (Exception e) {
                log.debug("Error extracting price with selector {}: {}", selector, e.getMessage());
            }
        }
        return Optional.empty();
    }
    
    /**
     * Checks if the text is likely to be a main product price (not a secondary price).
     * 
     * @param text the text to check
     * @return true if the text likely contains a main product price
     */
    protected boolean isLikelyMainPrice(String text) {
        // Skip excessively long text (likely contains multiple prices)
        if (text.length() > MAX_PRICE_TEXT_LENGTH) {
            return false;
        }
        
        // Avoid small prices (likely shipping or secondary prices)
        if (text.matches(".*\\d+.*")) {
            try {
                // Try to extract a number and see if it's in a reasonable range for a product
                String priceStr = text.replaceAll("[^\\d.]", "");
                double numValue = Double.parseDouble(priceStr);
                // Main product prices are typically > 100 in most currencies
                return numValue > 100.0;
            } catch (Exception ignored) {
                // If we can't parse it, still consider it
                return true;
            }
        }
        return true;
    }
} 