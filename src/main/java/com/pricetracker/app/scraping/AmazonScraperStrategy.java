package com.pricetracker.app.scraping;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Specialized scraper strategy for Amazon websites, with enhanced support for Amazon India.
 */
@Component
public class AmazonScraperStrategy implements ScraperStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(AmazonScraperStrategy.class);
    
    // Regular expression to extract price from text (handles various formats like ₹4,599.00, ₹4,599, 4599, etc.)
    // Enhanced to better handle Indian Rupee formats
    private static final Pattern PRICE_PATTERN = Pattern.compile("(?:₹|Rs\\.?|INR)?\\s*([\\d,]+(?:\\.\\d+)?)");
    
    // List of CSS selectors for Amazon price elements (in order of priority)
    // Enhanced with more India-specific selectors
    private static final List<String> PRICE_SELECTORS = Arrays.asList(
        // Amazon India specific selectors
        "#corePriceDisplay_desktop_feature_div .a-price .a-offscreen",
        "#corePriceDisplay_desktop_feature_div .a-price-whole",
        ".indiaPriceInfoButton span.a-size-base",
        ".currencyINR",
        "span.a-price-whole",
        "#priceblock_ourprice_row .a-span12 span.a-color-price",
        "#priceblock_dealprice_row .a-span12 span.a-color-price",
        "#apex_desktop_newAccordionRow span.a-price span.a-offscreen",
        "#apex_desktop_newAccordionRow span.a-price .a-price-whole",
        "#aod-price-1 span.a-offscreen",
        "#buybox-container .a-price .a-offscreen",
        
        // Prime Day Deal or Deal price selectors
        ".priceToPay .a-offscreen",
        ".apexPriceToPay .a-offscreen",
        
        // Regular price selectors
        "#priceblock_ourprice",
        "#priceblock_dealprice",
        ".a-price .a-offscreen",
        ".a-size-large.a-color-price",
        "#price_inside_buybox",
        "#newBuyBoxPrice",
        "#tp_price_block_total_price_ww",
        
        // Deal price selectors
        "#priceblock_saleprice",
        "#snsPrice .a-color-price",
        
        // Mobile selectors
        "#corePrice_feature_div .a-offscreen",
        "#corePrice_desktop .a-offscreen",
        
        // Kindle book price
        "#kindle-price",
        
        // Legacy selectors
        ".a-color-price",
        ".offer-price",
        ".sale-price",
        
        // Generic selectors
        ".a-price-whole",  // Often used for Indian Rupee integer part
        "#price",
        ".price",
        "#priceblock_ourprice_row span",
        "#dealprice_shippingmessage span",
        "[data-a-color='price'] span.a-offscreen"
    );
    
    // List of CSS selectors for Amazon product name elements
    private static final List<String> NAME_SELECTORS = Arrays.asList(
        // Amazon India specific selectors
        "h1#title span#productTitle",
        "h1.product-title-word-break",
        "#productTitle",
        "#title",
        ".product-title-word-break",
        ".a-size-large.product-title-word-break",
        "[data-feature-name='title']",
        "#item_name",
        "#ebooksProductTitle"
    );
    
    // List of CSS selectors for Amazon product image elements
    private static final List<String> IMAGE_SELECTORS = Arrays.asList(
        // Amazon India specific selectors
        "#landingImage",
        "#imgBlkFront",
        "#ebooksImgBlkFront",
        "#main-image",
        "#imgTagWrapperId img",
        ".a-dynamic-image",
        "#dealCardDynImage",
        ".image",
        // Product detail carousel images
        "#altImages .imageThumbnail img",
        ".imageSwatches img"
    );
    
    @Override
    public boolean canHandle(String url) {
        if (url == null) {
            return false;
        }
        
        return url.contains("amazon.in") ||
               url.contains("amazon.com") ||
               url.contains("amzn.in") || 
               url.contains("amzn.to") ||
               url.contains("a.co");
    }
    
    @Override
    public Optional<BigDecimal> extractPrice(Document doc) {
        log.debug("Extracting price using Amazon-specific strategy");
        
        // First try India-specific approach for Amazon.in
        if (doc.baseUri().contains("amazon.in")) {
            log.debug("Detected Amazon India site, trying India-specific extraction");
            Optional<BigDecimal> indiaPrice = extractIndianPrice(doc);
            if (indiaPrice.isPresent()) {
                return indiaPrice;
            }
        }
        
        // Try each selector in order
        for (String selector : PRICE_SELECTORS) {
            Optional<BigDecimal> price = extractPriceWithSelector(doc, selector);
            if (price.isPresent()) {
                return price;
            }
        }
        
        // Try to find any element that might contain a price by looking for currency symbols or price-like text
        return findPriceInPage(doc);
    }
    
    /**
     * Special extraction logic for Amazon India prices
     */
    private Optional<BigDecimal> extractIndianPrice(Document doc) {
        // Try specific price areas on Amazon India
        Elements priceElements = doc.select(
            ".indiaPriceInfoButton, " +
            "#corePriceDisplay_desktop_feature_div, " +
            "#corePrice_feature_div, " +
            "#apex_desktop, " +
            ".a-box-group .a-section span.a-color-price"
        );
        
        for (Element element : priceElements) {
            // Look for price text with Rupee symbol in or near this element
            Elements rupeeElements = element.select("*:contains(₹)");
            for (Element rupeeElement : rupeeElements) {
                String text = rupeeElement.text().trim();
                Optional<BigDecimal> price = parsePrice(text);
                if (price.isPresent()) {
                    log.debug("Found Amazon India price: {} from text '{}'", price.get(), text);
                    return price;
                }
            }
        }
        
        return Optional.empty();
    }
    
    private Optional<BigDecimal> extractPriceWithSelector(Document doc, String selector) {
        try {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                String priceText = element.text().trim();
                log.debug("Found potential price element with selector {}: '{}'", selector, priceText);
                return parsePrice(priceText);
            }
        } catch (Exception e) {
            log.debug("Error extracting price with selector {}: {}", selector, e.getMessage());
        }
        return Optional.empty();
    }
    
    private Optional<BigDecimal> findPriceInPage(Document doc) {
        // Look for elements containing price-related text, with enhanced India-specific patterns
        Elements priceElements = doc.select(
            "*:contains(₹), " + 
            "*:contains(Rs), " + 
            "*:contains(INR), " + 
            "*:contains(Price:), " + 
            "*:contains(MRP), " + 
            "*:contains(price), " + 
            "*:contains(Deal of the Day), " + 
            "*:contains(Limited time deal)"
        );
        
        for (Element element : priceElements) {
            if (!element.hasText() || element.children().size() > 5) {
                continue; // Skip elements without text or with too many children (likely not price elements)
            }
            
            String text = element.text().trim();
            
            // Skip long texts
            if (text.length() > 50) {
                continue;
            }
            
            Optional<BigDecimal> price = parsePrice(text);
            if (price.isPresent()) {
                log.debug("Found price {} in text: '{}'", price.get(), text);
                return price;
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Parse a price from text, handling various formats with enhanced support for Indian formats.
     */
    private Optional<BigDecimal> parsePrice(String text) {
        if (text == null || text.isEmpty()) {
            return Optional.empty();
        }
        
        // Special case for Indian Rupee formatting, which can vary:
        // ₹4,599.00 or ₹ 4,599.00 or ₹4,599 or ₹ 4,599 or Rs. 4,599 or Rs 4599
        try {
            Matcher matcher = PRICE_PATTERN.matcher(text);
            if (matcher.find()) {
                String priceStr = matcher.group(1).replace(",", ""); // Remove commas
                return Optional.of(new BigDecimal(priceStr));
            }
            
            // Additional check for price split into whole/fraction parts (common in India)
            // Example: "₹4,599 00" (where 00 is the fraction part, without a decimal point)
            if (text.contains("₹") && text.matches(".*\\d+.*")) {
                // Extract all numbers
                String[] parts = text.split("[^\\d]");
                StringBuilder priceBuilder = new StringBuilder();
                boolean foundMain = false;
                
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        if (!foundMain && part.length() > 1) {
                            // This is likely the main price
                            priceBuilder.append(part);
                            foundMain = true;
                        } else if (foundMain && part.length() <= 2) {
                            // This is likely the fraction part
                            priceBuilder.append(".").append(part);
                            break;
                        }
                    }
                }
                
                if (foundMain && priceBuilder.length() > 0) {
                    return Optional.of(new BigDecimal(priceBuilder.toString()));
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse price from text: '{}'", text);
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<String> extractName(Document doc) {
        log.debug("Extracting name using Amazon-specific strategy");
        
        // Try each selector in order
        for (String selector : NAME_SELECTORS) {
            try {
                Element element = doc.selectFirst(selector);
                if (element != null) {
                    String name = element.text().trim();
                    return Optional.of(name);
                }
            } catch (Exception e) {
                log.debug("Error extracting name with selector {}: {}", selector, e.getMessage());
            }
        }
        
        // Fallback to any h1 element
        Element h1 = doc.selectFirst("h1");
        if (h1 != null) {
            return Optional.of(h1.text().trim());
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<String> extractImageUrl(Document doc) {
        log.debug("Extracting image URL using Amazon-specific strategy");
        
        // Try each selector in order
        for (String selector : IMAGE_SELECTORS) {
            try {
                Element element = doc.selectFirst(selector);
                if (element != null) {
                    // Handle dynamic image JSON data (common on Amazon)
                    if (element.hasAttr("data-a-dynamic-image")) {
                        String jsonImages = element.attr("data-a-dynamic-image");
                        if (jsonImages.startsWith("{\"")) {
                            String imageUrl = jsonImages.substring(2, jsonImages.indexOf('"', 2));
                            return Optional.of(imageUrl);
                        }
                    }
                    
                    // Try data-old-hires attribute (high resolution)
                    if (element.hasAttr("data-old-hires")) {
                        return Optional.of(element.attr("data-old-hires"));
                    }
                    
                    // Try data-src attribute
                    if (element.hasAttr("data-src")) {
                        return Optional.of(element.attr("data-src"));
                    }
                    
                    // Default src attribute
                    if (element.hasAttr("src")) {
                        return Optional.of(element.attr("src"));
                    }
                }
            } catch (Exception e) {
                log.debug("Error extracting image URL with selector {}: {}", selector, e.getMessage());
            }
        }
        
        return Optional.empty();
    }
} 