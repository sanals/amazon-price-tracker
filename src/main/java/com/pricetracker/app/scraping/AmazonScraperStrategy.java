package com.pricetracker.app.scraping;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scraper strategy for Amazon websites, with special handling for Amazon India.
 */
@Component
public class AmazonScraperStrategy extends BaseScraperStrategy {
    
    // Max length for price text to avoid related products sections
    private static final int MAX_PRICE_TEXT_LENGTH = 100;
    
    // Regular expression to extract Indian price from text
    private static final Pattern INDIAN_PRICE_PATTERN = Pattern.compile("(?:₹|Rs\\.?|INR)?\\s*([\\d,]+(?:\\.\\d+)?)");
    
    // List of CSS selectors for Amazon price elements (in order of priority)
    private static final String[] PRICE_SELECTORS = {
        // Main price display (most reliable)
        ".a-price .a-offscreen", 
        ".a-section.a-spacing-none.aok-align-center .a-price .a-offscreen",
        
        // Amazon India specific price selectors
        ".a-price-whole", 
        "#priceblock_ourprice",
        "#priceblock_dealprice", 
        ".apexPriceToPay .a-offscreen",
        ".priceToPay .a-offscreen", 
        ".priceToPay span[data-a-size='xl']",
        
        // Deal prices
        "#dealprice_savings .a-offscreen",
        
        // Power adapter specific selectors based on the provided example
        "span.a-price span.a-offscreen", 
        "#corePrice_desktop .a-offscreen",
        
        // Fallbacks for various layouts
        ".a-color-price",
        ".a-text-price .a-offscreen",
        ".a-lineitem .a-color-price",
        "#usedBuySection .a-color-price"
    };
    
    // List of CSS selectors for Amazon product title elements
    private static final String[] NAME_SELECTORS = {
        "#productTitle",
        "#title",
        ".product-title-word-break",
        ".product-title",
        "h1.a-size-large",
        "[data-feature-name='title']"
    };
    
    // List of CSS selectors for Amazon product image elements
    private static final String[] IMAGE_SELECTORS = {
        "#landingImage",
        "#imgBlkFront",
        ".a-dynamic-image",
        "#main-image",
        "#imgTagWrapperId img",
        ".imgTagWrapper img"
    };
    
    @Override
    public boolean canHandle(String url) {
        return url != null && (
            url.contains("amazon.") || 
            url.contains("amzn.") || 
            url.contains("a.co")
        );
    }
    
    @Override
    public Optional<BigDecimal> extractPrice(Document doc) {
        if (isCaptchaPage(doc)) {
            log.warn("Detected CAPTCHA verification page during price extraction - aborting");
            return Optional.empty();
        }
        
        // 1. Try Amazon India price extraction first for Indian sites
        if (isAmazonIndia(doc)) {
            Optional<BigDecimal> indiaPrice = extractIndianPrice(doc);
            if (indiaPrice.isPresent()) {
                return indiaPrice;
            }
        }
        
        // 2. Try with our specific selectors
        Optional<BigDecimal> price = extractPriceWithSelectors(doc, PRICE_SELECTORS);
        if (price.isPresent()) {
            return price;
        }
        
        // 3. Try a different approach for Amazon - search for all elements with a price class
        for (Element element : doc.select("[class*=price], [class*=Price], [id*=price], [id*=Price]")) {
            String priceText = element.text().trim();
            
            // Skip texts that are likely not main product prices
            if (priceText.length() > MAX_PRICE_TEXT_LENGTH || priceText.isEmpty()) {
                continue;
            }
            
            log.debug("Found potential price element from generic selectors: '{}'", priceText);
            
            // Use the COMMON_PRICE_PATTERN from parent class via helper method
            Optional<BigDecimal> extractedPrice = tryParsePrice(priceText);
            if (extractedPrice.isPresent()) {
                return extractedPrice;
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<String> extractName(Document doc) {
        if (isCaptchaPage(doc)) {
            log.warn("Detected CAPTCHA verification page during name extraction - aborting");
            return Optional.empty();
        }
        
        for (String selector : NAME_SELECTORS) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                String name = element.text().trim();
                if (!name.isEmpty()) {
                    return Optional.of(name);
                }
            }
        }
        
        // Fallback to first h1 if no specific selector worked
        Element h1 = doc.selectFirst("h1");
        if (h1 != null && !h1.text().isEmpty()) {
            return Optional.of(h1.text().trim());
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<String> extractImageUrl(Document doc) {
        if (isCaptchaPage(doc)) {
            log.warn("Detected CAPTCHA verification page during image extraction - aborting");
            return Optional.empty();
        }
        
        for (String selector : IMAGE_SELECTORS) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                // Try different image attributes in order of preference
                if (element.hasAttr("data-old-hires")) {
                    return Optional.of(element.attr("data-old-hires"));
                } else if (element.hasAttr("data-a-dynamic-image")) {
                    String jsonImages = element.attr("data-a-dynamic-image");
                    // Extract the first URL from the JSON string (format: {"url1":[], "url2":[],...})
                    if (jsonImages.startsWith("{\"")) {
                        int firstQuote = jsonImages.indexOf('"');
                        int secondQuote = jsonImages.indexOf('"', firstQuote + 1);
                        if (firstQuote >= 0 && secondQuote > firstQuote) {
                            return Optional.of(jsonImages.substring(firstQuote + 1, secondQuote));
                        }
                    }
                } else if (element.hasAttr("src")) {
                    return Optional.of(element.attr("src"));
                }
            }
        }
        
        // Fallback approach - find any large image in the document
        Elements imgs = doc.select("img[width][height]");
        for (Element img : imgs) {
            try {
                int width = Integer.parseInt(img.attr("width"));
                int height = Integer.parseInt(img.attr("height"));
                if (width > 300 && height > 300 && img.hasAttr("src")) {
                    return Optional.of(img.attr("src"));
                }
            } catch (NumberFormatException ignored) {
                // Continue to next image if attributes aren't valid integers
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Helper method that uses the common price pattern but doesn't override parent method
     */
    private Optional<BigDecimal> tryParsePrice(String text) {
        if (text == null || text.isEmpty()) {
            return Optional.empty();
        }
        
        // Skip excessively long text
        if (text.length() > MAX_PRICE_TEXT_LENGTH) {
            log.debug("Skipping oversized price text (length {})", text.length());
            return Optional.empty();
        }
        
        try {
            // First try direct digit extraction - simplest and most reliable
            Matcher digitMatcher = Pattern.compile("(\\d{1,3}(,\\d{3})*(\\.\\d+)?)").matcher(text);
            if (digitMatcher.find()) {
                String priceStr = digitMatcher.group(1).replace(",", "");
                log.debug("Directly extracted price digits from text: '{}'", priceStr);
                return Optional.of(new BigDecimal(priceStr));
            }
            
            // If direct extraction fails, try cleaning
            String cleanedText = text;
            
            // Special handling for corrupted Rupee symbol
            if (text.contains("Γé╣") || text.contains("Γ") || text.contains("é╣")) {
                cleanedText = text.replace("Γé╣", "Rs.")
                               .replaceAll("[^\\x00-\\x7F]", ""); // Remove all non-ASCII chars
                log.debug("Aggressive clean of corrupted text: '{}'", cleanedText);
            }
            
            // Try the Indian format on cleaned text
            if (cleanedText.contains("₹") || cleanedText.contains("Rs.") || cleanedText.contains("INR")) {
                Matcher matcher = INDIAN_PRICE_PATTERN.matcher(cleanedText);
                if (matcher.find()) {
                    String priceStr = matcher.group(1).replace(",", ""); // Remove commas
                    return Optional.of(new BigDecimal(priceStr));
                }
            }
            
            // Last resort - just find any sequence of digits with optional commas and decimal point
            Matcher lastResortMatcher = Pattern.compile("(\\d+(?:,\\d+)*(?:\\.\\d+)?)").matcher(cleanedText);
            if (lastResortMatcher.find()) {
                String priceStr = lastResortMatcher.group(1).replace(",", "");
                log.debug("Last resort price extraction: '{}'", priceStr);
                return Optional.of(new BigDecimal(priceStr));
            }
        } catch (Exception e) {
            log.debug("Failed to parse price from text: '{}' - Error: {}", text, e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Special extraction logic for Amazon India prices
     */
    private Optional<BigDecimal> extractIndianPrice(Document doc) {
        log.debug("Extracting Amazon India price with enhanced strategy");
        
        // First check the power adapter specific selectors from the example
        Element dealPrice = doc.selectFirst(".a-section.a-spacing-none.aok-align-center .a-price .a-offscreen");
        if (dealPrice != null) {
            String dealPriceText = dealPrice.text().trim();
            log.debug("Found USB-C Power Adapter price: '{}'", dealPriceText);
            
            Optional<BigDecimal> price = tryParsePrice(dealPriceText);
            if (price.isPresent()) {
                return price;
            }
        }
        
        // Try specific BuyBox price location (where Add to Cart button is)
        Element buyBoxPrice = doc.selectFirst("#corePrice_desktop .a-offscreen, .priceToPay .a-offscreen");
        if (buyBoxPrice != null) {
            String buyBoxPriceText = buyBoxPrice.text().trim();
            log.debug("Found BuyBox price: '{}'", buyBoxPriceText);
            
            Optional<BigDecimal> price = tryParsePrice(buyBoxPriceText);
            if (price.isPresent()) {
                return price;
            }
        }
        
        // Try other price selectors with specific Indian rupee format check
        for (String selector : PRICE_SELECTORS) {
            Element element = doc.selectFirst(selector);
            if (element != null) {
                String priceText = element.text().trim();
                
                // Skip if text is too long (likely not a clean price)
                if (priceText.length() > MAX_PRICE_TEXT_LENGTH) {
                    continue;
                }
                
                // Look specifically for Indian rupee symbol, corrupted rupee symbol, or pattern
                if (priceText.contains("₹") || priceText.contains("Rs.") || 
                    priceText.contains("Γé╣") || // Check for corrupted Rupee symbol
                    priceText.matches(".*\\d+,\\d{3}.*")) {
                    log.debug("Found potential Indian price: '{}'", priceText);
                    
                    Optional<BigDecimal> price = tryParsePrice(priceText);
                    if (price.isPresent()) {
                        return price;
                    }
                }
            }
        }
        
        // Last resort - try to find any text with the corrupted Rupee symbol pattern
        Elements potentialPriceElements = doc.select("*:containsOwn(Γé╣)");
        for (Element element : potentialPriceElements) {
            String priceText = element.text().trim();
            if (priceText.length() <= MAX_PRICE_TEXT_LENGTH) {
                log.debug("Found text with corrupted Rupee symbol: '{}'", priceText);
                Optional<BigDecimal> price = tryParsePrice(priceText);
                if (price.isPresent()) {
                    return price;
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Check if the document is from Amazon India
     */
    private boolean isAmazonIndia(Document doc) {
        String url = doc.baseUri();
        return url != null && (url.contains("amazon.in") || url.contains(".in/"));
    }
    
    @Override
    public boolean isCaptchaPage(Document doc) {
        // Check for common CAPTCHA page elements
        boolean hasCaptchaTitle = !doc.select("title:contains(Robot Check), title:contains(CAPTCHA), title:contains(Enter the characters)").isEmpty();
        boolean hasCaptchaImage = !doc.select("img[src*=captcha], form[action*=validateCaptcha]").isEmpty();
        boolean hasCaptchaText = !doc.select("h4:contains(Enter the characters), p:contains(not a robot), h4:contains(Type the characters)").isEmpty();
        boolean hasVerificationForm = !doc.select("form[action*=verify]").isEmpty();
        
        boolean result = hasCaptchaTitle || hasCaptchaImage || hasCaptchaText || hasVerificationForm;
        
        if (result) {
            log.warn("Detected CAPTCHA page with the following signals: title={}, image={}, text={}, form={}", 
                    hasCaptchaTitle, hasCaptchaImage, hasCaptchaText, hasVerificationForm);
        }
        
        return result;
    }
    
    /**
     * Helper method to extract all price texts from a document for debugging purposes.
     */
    public Map<String, String> extractAllPriceTexts(Document doc) {
        Map<String, String> priceTexts = new HashMap<>();
        
        if (isCaptchaPage(doc)) {
            priceTexts.put("error", "CAPTCHA page detected");
            return priceTexts;
        }
        
        // Try all our selectors and collect values
        for (String selector : PRICE_SELECTORS) {
            Elements elements = doc.select(selector);
            if (!elements.isEmpty()) {
                for (Element element : elements) {
                    String text = element.text().trim();
                    if (!text.isEmpty() && text.length() < MAX_PRICE_TEXT_LENGTH) {
                        priceTexts.put(selector, text);
                    }
                }
            }
        }
        
        return priceTexts;
    }
} 