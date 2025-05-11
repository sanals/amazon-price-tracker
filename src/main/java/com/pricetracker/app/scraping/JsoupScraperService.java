package com.pricetracker.app.scraping;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.HttpStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of ScraperService using Jsoup library and strategy pattern for site-specific scraping.
 */
@Service
public class JsoupScraperService implements ScraperService {
    
    private static final Logger log = LoggerFactory.getLogger(JsoupScraperService.class);
    private static final Random random = new Random();
    
    // List of registered scraper strategies (thread-safe)
    private final List<ScraperStrategy> scraperStrategies = new CopyOnWriteArrayList<>();
    
    // Common browser user agents for rotation
    private static final String[] BROWSER_USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Safari/605.1.15",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:90.0) Gecko/20100101 Firefox/90.0"
    };
    
    @Value("${app.scraper.default-delay-ms:1000}")
    private int defaultDelayMs;
    
    /**
     * Constructor to initialize with required strategies.
     */
    @Autowired
    public JsoupScraperService(AmazonScraperStrategy amazonScraperStrategy) {
        registerStrategy(amazonScraperStrategy);
        log.info("JsoupScraperService initialized with {} strategies", scraperStrategies.size());
    }
    
    @Override
    public void registerStrategy(ScraperStrategy strategy) {
        scraperStrategies.add(strategy);
        log.info("Registered scraper strategy: {}", strategy.getClass().getSimpleName());
    }
    
    @Override
    public List<ScraperStrategy> getStrategies() {
        return new ArrayList<>(scraperStrategies);
    }
    
    @Override
    public Optional<ScraperStrategy> findStrategyForUrl(String url) {
        if (url == null || url.isEmpty()) {
            return Optional.empty();
        }
        
        // First expand shortened URLs
        String expandedUrl = expandShortenedUrl(url);
        if (!expandedUrl.equals(url)) {
            log.debug("URL expanded from {} to {}", url, expandedUrl);
            url = expandedUrl;
        }
        
        // Find first strategy that can handle this URL
        for (ScraperStrategy strategy : scraperStrategies) {
            if (strategy.canHandle(url)) {
                log.debug("Found strategy {} for URL: {}", 
                        strategy.getClass().getSimpleName(), url);
                return Optional.of(strategy);
            }
        }
        
        log.debug("No specific strategy found for URL: {}", url);
        return Optional.empty();
    }
    
    @Override
    public Optional<BigDecimal> scrapePrice(String productUrl) {
        try {
            Document doc = fetchDocument(productUrl);
            
            // Find appropriate strategy
            Optional<ScraperStrategy> strategyOpt = findStrategyForUrl(productUrl);
            if (strategyOpt.isPresent()) {
                log.debug("Using {} for URL: {}", 
                        strategyOpt.get().getClass().getSimpleName(), productUrl);
                return strategyOpt.get().extractPrice(doc);
            }
            
            // If no specific strategy, use generic price extraction
            log.debug("No specific strategy found, using generic price extraction for URL: {}", productUrl);
            return extractGenericPrice(doc);
        } catch (Exception e) {
            logScrapingError("price", productUrl, e);
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<ProductDetails> scrapeProductDetails(String productUrl) {
        try {
            Document doc = fetchDocument(productUrl);
            
            // Find appropriate strategy
            Optional<ScraperStrategy> strategyOpt = findStrategyForUrl(productUrl);
            if (strategyOpt.isPresent()) {
                log.debug("Using {} for URL: {}", 
                        strategyOpt.get().getClass().getSimpleName(), productUrl);
                return strategyOpt.get().scrapeProductDetails(doc);
            }
            
            // If no specific strategy, build details from generic extraction
            log.debug("No specific strategy found, using generic extraction for URL: {}", productUrl);
            return Optional.of(new ProductDetails(
                extractGenericName(doc),
                extractGenericImageUrl(doc),
                extractGenericPrice(doc)
            ));
        } catch (Exception e) {
            logScrapingError("product details", productUrl, e);
            return Optional.empty();
        }
    }
    
    @Override
    public Document fetchDocument(String url) throws IOException {
        // First expand shortened URLs
        String expandedUrl = expandShortenedUrl(url);
        if (!expandedUrl.equals(url)) {
            log.debug("URL expanded from {} to {}", url, expandedUrl);
            url = expandedUrl;
        }
        
        // Add random delay to avoid detection (between 1-3 seconds by default)
        try {
            int delay = defaultDelayMs + random.nextInt(defaultDelayMs);
            log.debug("Adding delay of {}ms before request", delay);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Select a random user agent
        String userAgent = getRandomUserAgent();
        
        try {
            // Create a map of headers that mimic a real browser
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
            headers.put("Accept-Language", "en-US,en;q=0.5");
            headers.put("Accept-Charset", "utf-8");
            headers.put("Referer", "https://www.google.com/");
            headers.put("DNT", "1");
            headers.put("Connection", "keep-alive");
            headers.put("Upgrade-Insecure-Requests", "1");
            headers.put("Sec-Fetch-Dest", "document");
            headers.put("Sec-Fetch-Mode", "navigate");
            headers.put("Sec-Fetch-Site", "cross-site");
            headers.put("Pragma", "no-cache");
            headers.put("Cache-Control", "no-cache");
            
            log.debug("Fetching document from URL: {} with user agent: {}", url, userAgent);
            
            Document doc = Jsoup.connect(url)
                .userAgent(userAgent)
                .headers(headers)
                .timeout(15000) // Extended timeout (15 seconds)
                .followRedirects(true)
                .maxBodySize(0) // Unlimited body size
                .ignoreContentType(false)
                .ignoreHttpErrors(false)
                .get();
            
            // Check if this is a CAPTCHA page
            for (ScraperStrategy strategy : scraperStrategies) {
                if (strategy.canHandle(url) && strategy.isCaptchaPage(doc)) {
                    log.warn("Detected CAPTCHA page for URL: {}", url);
                    throw new ScrapingException("CAPTCHA verification required for URL: " + url);
                }
            }
            
            return doc;
        } catch (ScrapingException e) {
            throw e;
        } catch (SocketTimeoutException e) {
            log.warn("Connection timed out for URL: {}", url);
            throw e;
        } catch (UnknownHostException e) {
            log.warn("Unknown host for URL: {}", url);
            throw e;
        } catch (HttpStatusException e) {
            log.warn("HTTP error {} for URL: {}", e.getStatusCode(), url);
            throw e;
        } catch (IOException e) {
            log.warn("IO error for URL: {}", url, e);
            throw e;
        }
    }
    
    @Override
    public String expandShortenedUrl(String shortenedUrl) {
        // Find a strategy that can handle this URL
        for (ScraperStrategy strategy : scraperStrategies) {
            if (strategy.canHandle(shortenedUrl)) {
                String expandedUrl = strategy.expandShortenedUrl(shortenedUrl);
                if (!expandedUrl.equals(shortenedUrl)) {
                    return expandedUrl;
                }
            }
        }
        
        // Fall back to default implementation in BaseScraperStrategy
        try {
            // Use our BaseScraperStrategy's implementation as a fallback
            ScraperStrategy defaultStrategy = scraperStrategies.stream()
                .filter(s -> s instanceof BaseScraperStrategy)
                .findFirst()
                .orElse(null);
            
            if (defaultStrategy != null) {
                return defaultStrategy.expandShortenedUrl(shortenedUrl);
            }
        } catch (Exception e) {
            log.warn("Error expanding URL {}: {}", shortenedUrl, e.getMessage());
        }
        
        return shortenedUrl; // Return original if expansion fails
    }
    
    private String getRandomUserAgent() {
        // Use a random browser-like user agent
        return BROWSER_USER_AGENTS[random.nextInt(BROWSER_USER_AGENTS.length)];
    }
    
    /**
     * Generic price extraction for sites without a specific strategy
     */
    private Optional<BigDecimal> extractGenericPrice(Document doc) {
        try {
            // Try a variety of common price selectors used by popular e-commerce sites
            String selectors = "#price, .price, [data-price], .product-price, .current-price, .price-current, " +
                "span.price, div.price, span[itemprop=price], [class*=price]:not(del):not(s), " +
                ".regular-price, .offer-price, .sale-price, .our-price, .special-price";
            
            for (String selector : selectors.split(", ")) {
                var elements = doc.select(selector);
                if (!elements.isEmpty()) {
                    String priceText = elements.first().text()
                        .replaceAll("[^\\d.,]", "") // Remove non-numeric characters except . and ,
                        .replace(",", "."); // Normalize decimal separator
                    
                    // Additional handling for multiple dots (e.g., 1.234.56)
                    if (priceText.indexOf('.') != priceText.lastIndexOf('.')) {
                        priceText = priceText.replaceAll("\\.(?=.*\\.)", "");
                    }
                    
                    return Optional.of(new BigDecimal(priceText));
                }
            }
            
            // If no match found using selectors, try looking for currency symbols
            for (var element : doc.select("*:containsOwn($), *:containsOwn(€), *:containsOwn(£), " +
                    "*:containsOwn(¥), *:containsOwn(₹), *:containsOwn(price), *:containsOwn(Price)")) {
                String text = element.text().trim();
                // Check if it has digits and common price patterns
                if (text.matches(".*\\d+.*") && 
                    (text.contains("$") || text.contains("€") || text.contains("£") || 
                     text.contains("¥") || text.contains("₹") || 
                     text.toLowerCase().contains("price"))) {
                    
                    String priceText = text.replaceAll("[^\\d.,]", "")
                                          .replace(",", ".");
                    
                    // Additional handling for multiple dots
                    if (priceText.indexOf('.') != priceText.lastIndexOf('.')) {
                        priceText = priceText.replaceAll("\\.(?=.*\\.)", "");
                    }
                    
                    try {
                        return Optional.of(new BigDecimal(priceText));
                    } catch (NumberFormatException e) {
                        // Continue to next element if parsing fails
                    }
                }
            }
            
            log.debug("No price element found in document");
            return Optional.empty();
        } catch (Exception e) {
            log.debug("Failed to extract price from document: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Generic name extraction for sites without a specific strategy
     */
    private Optional<String> extractGenericName(Document doc) {
        try {
            // Try a variety of common name/title selectors used by popular e-commerce sites
            String selectors = "h1, #title, .product-title, .product-name, .title, " +
                               "h1[itemprop=name], [itemprop=name], .page-title, .heading";
            
            for (String selector : selectors.split(", ")) {
                var elements = doc.select(selector);
                if (!elements.isEmpty()) {
                    return Optional.of(elements.first().text().trim());
                }
            }
            
            // Fallback to page title if no product name found
            return Optional.of(doc.title().trim());
        } catch (Exception e) {
            log.debug("Failed to extract name from document: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Generic image URL extraction for sites without a specific strategy
     */
    private Optional<String> extractGenericImageUrl(Document doc) {
        try {
            // Try a variety of common image selectors used by popular e-commerce sites
            String selectors = "#main-image, .main-image, .product-image, .primary-image, " +
                               "[itemprop=image], img.product, .product-img, .hero-image, " +
                               ".gallery-image, .featured-image";
            
            for (String selector : selectors.split(", ")) {
                var elements = doc.select(selector);
                if (!elements.isEmpty()) {
                    return Optional.of(elements.first().attr("src"));
                }
            }
            
            // Look for large images on the page
            for (var img : doc.select("img")) {
                if (img.hasAttr("src") && 
                    !img.attr("src").isEmpty() && 
                    (img.hasAttr("width") && Integer.parseInt(img.attr("width")) > 200 || 
                     img.hasAttr("height") && Integer.parseInt(img.attr("height")) > 200)) {
                    return Optional.of(img.attr("src"));
                }
            }
            
            // If still not found, just get the first meaningful image
            for (var img : doc.select("img")) {
                if (img.hasAttr("src") && 
                    !img.attr("src").isEmpty() && 
                    !img.attr("src").endsWith(".gif") &&
                    !img.attr("src").contains("icon") &&
                    !img.attr("src").contains("logo")) {
                    return Optional.of(img.attr("src"));
                }
            }
            
            log.debug("No image element found in document");
            return Optional.empty();
        } catch (Exception e) {
            log.debug("Failed to extract image URL from document: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    private void logScrapingError(String type, String url, Exception e) {
        if (e instanceof HttpStatusException) {
            log.warn("HTTP {} error while scraping {} from URL: {}", 
                ((HttpStatusException) e).getStatusCode(), type, url);
        } else if (e instanceof SocketTimeoutException) {
            log.warn("Timeout while scraping {} from URL: {}", type, url);
        } else if (e instanceof UnknownHostException) {
            log.warn("Unknown host while scraping {} from URL: {}", type, url);
        } else if (e instanceof ScrapingException) {
            log.warn("Scraping error while scraping {} from URL: {}: {}", 
                type, url, e.getMessage());
        } else {
            log.warn("Error while scraping {} from URL: {}", type, url, e);
        }
    }
} 