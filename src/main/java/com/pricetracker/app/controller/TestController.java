package com.pricetracker.app.controller;

import com.pricetracker.app.dto.response.ApiResponse;
import com.pricetracker.app.entity.PriceHistory;
import com.pricetracker.app.entity.Product;
import com.pricetracker.app.repository.PriceHistoryRepository;
import com.pricetracker.app.repository.ProductRepository;
import com.pricetracker.app.scraping.AmazonScraperStrategy;
import com.pricetracker.app.scraping.ProductDetails;
import com.pricetracker.app.scraping.ScraperService;
import com.pricetracker.app.scraping.ScraperStrategy;
import com.pricetracker.app.scheduling.PriceCheckScheduler;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Test controller for debugging purposes.
 */
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);
    private static final int MAX_PRICE_TEXT_LENGTH = 100; // Max length for raw price text
    
    private final ScraperService scraperService;
    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PriceCheckScheduler priceCheckScheduler;
    private final AmazonScraperStrategy amazonScraperStrategy;

    /**
     * Test endpoint to check if scraping works from a given URL.
     * 
     * @param url the URL to scrape
     * @return scraped product details
     */
    @GetMapping("/scrape")
    public ApiResponse<Map<String, Object>> testScrape(@RequestParam String url) {
        Map<String, Object> result = new HashMap<>();
        
        // Test scraping just the price
        Optional<BigDecimal> priceResult = scraperService.scrapePrice(url);
        result.put("priceResult", priceResult.isPresent() ? priceResult.get() : "Not found");
        
        // Test scraping full product details
        Optional<ProductDetails> detailsResult = scraperService.scrapeProductDetails(url);
        if (detailsResult.isPresent()) {
            ProductDetails details = detailsResult.get();
            result.put("name", details.name().isPresent() ? details.name().get() : "Not found");
            result.put("imageUrl", details.imageUrl().isPresent() ? details.imageUrl().get() : "Not found");
            result.put("price", details.price().isPresent() ? details.price().get() : "Not found");
        } else {
            result.put("detailsResult", "Failed to scrape product details");
        }
        
        return ApiResponse.success(result, "Scrape test completed");
    }
    
    /**
     * Test endpoint specifically for Amazon India scraper functionality.
     * Can test with a URL or an HTML file
     * 
     * @param url Optional URL to test (if not provided, will use sample file)
     * @param useFile Whether to use a local HTML file for testing
     * @return scraped details using the AmazonScraperStrategy
     */
    @GetMapping("/scrape-amazon-india")
    public ApiResponse<Map<String, Object>> testAmazonIndiaScraper(
            @RequestParam(required = false) String url,
            @RequestParam(defaultValue = "false") boolean useFile) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            Document doc;
            if (useFile) {
                // Use a local HTML file for testing
                File htmlFile = new File("amazon_product.html");
                if (!htmlFile.exists()) {
                    return ApiResponse.error("Test file 'amazon_product.html' not found", 404);
                }
                doc = Jsoup.parse(htmlFile, "UTF-8", "https://www.amazon.in/dp/sample");
                result.put("source", "Local HTML file: " + htmlFile.getAbsolutePath());
            } else if (url != null && !url.isEmpty()) {
                // Use the provided URL
                doc = Jsoup.connect(url)
                     .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                     .timeout(10000)
                     .get();
                result.put("source", "URL: " + url);
            } else {
                return ApiResponse.error("Either URL or useFile=true must be provided", 400);
            }
            
            // Test if the strategy can handle this URL
            boolean canHandle = amazonScraperStrategy.canHandle(url);
            result.put("canHandle", canHandle);
            
            // Extract product details using the Amazon strategy
            Optional<String> name = amazonScraperStrategy.extractName(doc);
            Optional<String> imageUrl = amazonScraperStrategy.extractImageUrl(doc);
            Optional<BigDecimal> price = amazonScraperStrategy.extractPrice(doc);
            
            result.put("name", name.orElse("Not found"));
            result.put("imageUrl", imageUrl.orElse("Not found"));
            result.put("price", price.orElse(null));
            
            return ApiResponse.success(result, "Amazon India scraper test completed");
        } catch (IOException e) {
            return ApiResponse.error("Error fetching or parsing document: " + e.getMessage(), 500);
        } catch (Exception e) {
            return ApiResponse.error("Error during scraping: " + e.getMessage(), 500);
        }
    }
    
    /**
     * Test endpoint to manually save a test product with price history
     * 
     * @param url the URL to scrape
     * @return the saved product info and price history
     */
    @PostMapping("/save-product")
    public ApiResponse<Map<String, Object>> testSaveProduct(@RequestParam String url) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // First scrape the product details
            Optional<ProductDetails> detailsResult = scraperService.scrapeProductDetails(url);
            
            if (detailsResult.isEmpty() || detailsResult.get().price().isEmpty()) {
                return ApiResponse.error("Failed to scrape valid product details", 400);
            }
            
            ProductDetails details = detailsResult.get();
            BigDecimal price = details.price().get();
            String name = details.name().orElse("Unknown Product");
            String imageUrl = details.imageUrl().orElse(null);
            
            // Create or update product
            Product product = productRepository.findByProductUrl(url).orElse(new Product());
            product.setProductUrl(url);
            product.setName(name);
            product.setImageUrl(imageUrl);
            
            // Only record price history if price is different
            boolean isPriceChanged = product.getLastCheckedPrice() == null || 
                product.getLastCheckedPrice().compareTo(price) != 0;
                
            product.setLastCheckedPrice(price);
            product = productRepository.save(product);
            
            result.put("product", Map.of(
                "id", product.getId(),
                "url", product.getProductUrl(),
                "name", product.getName(),
                "price", product.getLastCheckedPrice()
            ));
            
            // Record price history if needed
            if (isPriceChanged) {
                PriceHistory priceHistory = new PriceHistory();
                priceHistory.setProduct(product);
                priceHistory.setPrice(price);
                priceHistory.setTimestamp(Instant.now());
                priceHistory = priceHistoryRepository.save(priceHistory);
                
                result.put("priceHistory", Map.of(
                    "id", priceHistory.getId(),
                    "price", priceHistory.getPrice(),
                    "timestamp", priceHistory.getTimestamp()
                ));
            } else {
                result.put("priceHistory", "No price change detected, history not updated");
            }
            
            return ApiResponse.success(result, "Product saved successfully");
        } catch (Exception e) {
            return ApiResponse.error("Error saving product: " + e.getMessage(), 500);
        }
    }
    
    /**
     * Test endpoint to manually trigger price check scheduler
     * 
     * @return results of the price check operation
     */
    @PostMapping("/run-scheduler")
    public ApiResponse<String> testRunScheduler() {
        try {
            priceCheckScheduler.checkPrices();
            return ApiResponse.success("Scheduler ran successfully");
        } catch (Exception e) {
            return ApiResponse.error("Error running scheduler: " + e.getMessage(), 500);
        }
    }
    
    /**
     * Get the latest price history entries for diagnostic purposes
     * 
     * @param limit number of entries to return
     * @return the latest price history entries
     */
    @GetMapping("/latest-prices")
    public ApiResponse<List<Map<String, Object>>> getLatestPrices(@RequestParam(defaultValue = "10") int limit) {
        // Use PageRequest with sort instead of a custom repository method
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<PriceHistory> latestPrices = priceHistoryRepository.findAll(pageRequest).getContent();
        
        List<Map<String, Object>> result = latestPrices.stream()
            .map(ph -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("id", ph.getId());
                entry.put("productId", ph.getProduct().getId());
                entry.put("productName", ph.getProduct().getName());
                entry.put("price", ph.getPrice());
                entry.put("timestamp", ph.getTimestamp());
                return entry;
            })
            .toList();
            
        return ApiResponse.success(result, "Latest price history retrieved");
    }

    /**
     * Test endpoint specifically for Amazon India shortened URLs.
     * This endpoint handles the CAPTCHA issue and expanded URL tracking.
     * 
     * @param url the Amazon India shortened URL to test
     * @return detailed information about the scraping attempt
     */
    @GetMapping("/scrape-amazon-shortened")
    public ApiResponse<Map<String, Object>> testAmazonShortenedUrl(@RequestParam String url) {
        Map<String, Object> result = new HashMap<>();
        result.put("originalUrl", url);
        
        try {
            // First check if this is an Amazon URL
            if (!amazonScraperStrategy.canHandle(url)) {
                return ApiResponse.error("URL is not a valid Amazon URL: " + url, 400);
            }
            
            result.put("isAmazonUrl", true);
            
            // Try to expand the shortened URL if needed
            String expandedUrl = url;
            if (url.contains("amzn.in") || url.contains("a.co")) {
                try {
                    URL shortenedUrl = new URI(url).toURL();
                    HttpURLConnection connection = (HttpURLConnection) shortenedUrl.openConnection();
                    connection.setInstanceFollowRedirects(false);
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                    connection.setRequestMethod("HEAD");
                    
                    int responseCode = connection.getResponseCode();
                    result.put("redirectResponseCode", responseCode);
                    
                    if (responseCode >= 300 && responseCode < 400) {
                        expandedUrl = connection.getHeaderField("Location");
                        result.put("expandedUrl", expandedUrl);
                    } else {
                        result.put("expansionError", "Could not expand URL, response code: " + responseCode);
                    }
                    
                    connection.disconnect();
                } catch (Exception e) {
                    result.put("urlExpansionError", e.getMessage());
                }
            }
            
            // Try to fetch the document
            Document doc = Jsoup.connect(expandedUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(15000)
                .followRedirects(true)
                .get();
            
            // Check if we hit a CAPTCHA page
            boolean isCaptcha = amazonScraperStrategy.isCaptchaPage(doc);
            result.put("isCaptchaPage", isCaptcha);
            
            if (isCaptcha) {
                result.put("pageTitle", doc.title());
                return ApiResponse.success(result, "CAPTCHA page detected - Amazon is blocking the scraper");
            }
            
            // Try to extract product details
            Optional<String> name = amazonScraperStrategy.extractName(doc);
            Optional<String> imageUrl = amazonScraperStrategy.extractImageUrl(doc);
            Optional<BigDecimal> price = amazonScraperStrategy.extractPrice(doc);
            
            result.put("extractedName", name.orElse("Not found"));
            result.put("extractedImageUrl", imageUrl.orElse("Not found"));
            result.put("extractedPrice", price.orElse(null));
            
            // Show debug info about the price selectors
            Map<String, String> priceElements = new HashMap<>();
            for (String selector : new String[] {
                    ".priceToPay .a-offscreen", 
                    ".apexPriceToPay .a-offscreen",
                    "#corePrice_feature_div .a-price .a-offscreen",
                    "#corePriceDisplay_desktop_feature_div .a-price .a-offscreen",
                    "#priceblock_ourprice", 
                    ".a-price .a-offscreen"
                }) {
                Element element = doc.selectFirst(selector);
                if (element != null) {
                    priceElements.put(selector, element.text());
                }
            }
            result.put("priceElementsFound", priceElements);
            
            return ApiResponse.success(result, "Amazon India shortened URL test completed");
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ApiResponse.error("Error testing Amazon shortened URL: " + e.getMessage(), 500);
        }
    }

    /**
     * Test endpoint specifically for power adapter products on Amazon India
     * 
     * @param url URL of the power adapter to test, default is the 20W USB-C Power Adapter
     * @return detailed scraping diagnostics
     */
    @GetMapping("/scrape-power-adapter")
    public ApiResponse<Map<String, Object>> testPowerAdapterScraping(
            @RequestParam(defaultValue = "https://amzn.in/d/gCtpi9n") String url) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("url", url);
        
        try {
            log.info("Testing scraping for power adapter at URL: {}", url);
            
            // Try to expand the URL if it's shortened
            String expandedUrl = url;
            if (url.contains("amzn.in") || url.contains("a.co")) {
                try {
                    URL shortenedUrl = new URI(url).toURL();
                    HttpURLConnection connection = (HttpURLConnection) shortenedUrl.openConnection();
                    connection.setInstanceFollowRedirects(false);
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                    connection.setRequestMethod("HEAD");
                    
                    int responseCode = connection.getResponseCode();
                    result.put("redirectResponseCode", responseCode);
                    
                    if (responseCode >= 300 && responseCode < 400) {
                        expandedUrl = connection.getHeaderField("Location");
                        result.put("expandedUrl", expandedUrl);
                    }
                    
                    connection.disconnect();
                } catch (Exception e) {
                    result.put("urlExpansionError", e.getMessage());
                }
            }
            
            // Fetch the document
            Document doc = Jsoup.connect(expandedUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(15000)
                .followRedirects(true)
                .get();
            
            // Get page title for verification
            result.put("pageTitle", doc.title());
            
            // Check if page is CAPTCHA
            boolean isCaptcha = amazonScraperStrategy.isCaptchaPage(doc);
            result.put("isCaptchaPage", isCaptcha);
            
            if (isCaptcha) {
                return ApiResponse.success(result, "CAPTCHA page detected");
            }
            
            // Scrape product details
            Optional<BigDecimal> scrapedPrice = amazonScraperStrategy.extractPrice(doc);
            Optional<String> scrapedName = amazonScraperStrategy.extractName(doc);
            Optional<String> scrapedImageUrl = amazonScraperStrategy.extractImageUrl(doc);
            
            result.put("name", scrapedName.orElse("Not found"));
            result.put("price", scrapedPrice.orElse(null));
            result.put("imageUrl", scrapedImageUrl.orElse("Not found"));
            
            // Add detailed price selectors diagnostics
            Map<String, String> priceSelectors = new HashMap<>();
            
            // Check standard selectors
            String[] selectorsToCheck = new String[] {
                ".priceToPay .a-offscreen", 
                ".apexPriceToPay .a-offscreen",
                ".a-section > span.a-price > span.a-offscreen",
                "#corePrice_feature_div .a-price .a-offscreen",
                "span.a-price-whole",
                ".a-price .a-offscreen"
            };
            
            for (String selector : selectorsToCheck) {
                Element element = doc.selectFirst(selector);
                if (element != null) {
                    priceSelectors.put(selector, element.text());
                }
            }
            
            result.put("matchedPriceSelectors", priceSelectors);
            
            // Also show just raw price text matches
            Elements allPriceTexts = doc.select("*:contains(₹)");
            List<String> rawPrices = new ArrayList<>();
            
            for (Element element : allPriceTexts) {
                String text = element.ownText().trim();
                if (text.contains("₹") && text.length() < MAX_PRICE_TEXT_LENGTH && text.matches(".*\\d+.*")) {
                    rawPrices.add(text);
                }
            }
            
            result.put("rawPriceTexts", rawPrices);
            
            // Test if we can scrape using our normal method too
            Optional<BigDecimal> serviceScrapeResult = scraperService.scrapePrice(url);
            result.put("serviceScrapedPrice", serviceScrapeResult.orElse(null));
            
            return ApiResponse.success(result, "Power adapter scraping test completed");
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ApiResponse.error("Error testing power adapter scraping: " + e.getMessage(), 500);
        }
    }

    /**
     * Get diagnostics from all registered scrapers for a URL
     * 
     * @param url the URL to test with all registered scrapers
     * @return results from each scraper
     */
    @GetMapping("/compare-scrapers")
    public ApiResponse<Map<String, Object>> compareScrapers(@RequestParam String url) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> scraperResults = new ArrayList<>();
        
        log.info("Testing URL with all registered scrapers: {}", url);
        result.put("url", url);
        
        try {
            // Get the document once to avoid multiple network requests
            Document doc = null;
            try {
                doc = scraperService.fetchDocument(url);
                result.put("fetchSuccess", true);
            } catch (Exception e) {
                log.error("Error fetching document: {}", e.getMessage());
                result.put("fetchSuccess", false);
                result.put("fetchError", e.getMessage());
                return ApiResponse.error("Failed to fetch document: " + e.getMessage());
            }
            
            // Get all registered strategies
            List<ScraperStrategy> strategies = scraperService.getStrategies();
            result.put("registeredScraperCount", strategies.size());
            
            // Test each strategy
            for (ScraperStrategy strategy : strategies) {
                Map<String, Object> strategyResult = new HashMap<>();
                String name = strategy.getClass().getSimpleName();
                strategyResult.put("name", name);
                strategyResult.put("canHandle", strategy.canHandle(url));
                
                if (strategy.canHandle(url)) {
                    // Check if it's a CAPTCHA page
                    strategyResult.put("captchaDetected", strategy.isCaptchaPage(doc));
                    
                    // Extract values if no CAPTCHA
                    if (!strategy.isCaptchaPage(doc)) {
                        Optional<BigDecimal> price = strategy.extractPrice(doc);
                        Optional<String> productName = strategy.extractName(doc);
                        Optional<String> imageUrl = strategy.extractImageUrl(doc);
                        
                        strategyResult.put("foundPrice", price.isPresent());
                        price.ifPresent(p -> strategyResult.put("price", p.toString()));
                        
                        strategyResult.put("foundName", productName.isPresent());
                        productName.ifPresent(n -> strategyResult.put("name", n));
                        
                        strategyResult.put("foundImage", imageUrl.isPresent());
                        imageUrl.ifPresent(i -> strategyResult.put("imageUrl", i));
                    }
                }
                
                scraperResults.add(strategyResult);
            }
            
            result.put("scraperResults", scraperResults);
            return ApiResponse.success(result, "Scraper comparison completed");
            
        } catch (Exception e) {
            log.error("Error comparing scrapers: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
            return ApiResponse.error("Error comparing scrapers: " + e.getMessage());
        }
    }

    /**
     * Test endpoint specifically for testing the updated Amazon India power adapter price scraping
     * 
     * @param url URL of the power adapter to test, defaults to the USB-C Power Adapter
     * @return detailed scraping results
     */
    @GetMapping("/test-power-adapter")
    public ApiResponse<Map<String, Object>> testPowerAdapter(
            @RequestParam(defaultValue = "https://amzn.in/d/39Vxu0p") String url) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("url", url);
        log.info("Testing power adapter scraping for: {}", url);
        
        try {
            // Fetch the document
            Document doc = scraperService.fetchDocument(url);
            result.put("fetchSuccess", true);
            
            // Get the strategy for Amazon
            Optional<ScraperStrategy> strategyOpt = scraperService.findStrategyForUrl(url);
            if (strategyOpt.isEmpty()) {
                result.put("error", "No suitable strategy found for URL");
                return ApiResponse.error("No suitable strategy found");
            }
            
            ScraperStrategy strategy = strategyOpt.get();
            result.put("strategy", strategy.getClass().getSimpleName());
            
            // Check for CAPTCHA
            boolean isCaptcha = strategy.isCaptchaPage(doc);
            result.put("captchaDetected", isCaptcha);
            
            if (isCaptcha) {
                result.put("warning", "CAPTCHA page detected, extraction may fail");
            }
            
            // Extract price
            Optional<BigDecimal> price = strategy.extractPrice(doc);
            result.put("priceFound", price.isPresent());
            price.ifPresent(p -> result.put("price", p.toString()));
            
            // Extract name
            Optional<String> name = strategy.extractName(doc);
            result.put("nameFound", name.isPresent());
            name.ifPresent(n -> result.put("name", n));
            
            // Extract image URL
            Optional<String> imageUrl = strategy.extractImageUrl(doc);
            result.put("imageFound", imageUrl.isPresent());
            imageUrl.ifPresent(i -> result.put("imageUrl", i));
            
            // Get all selectors used for debug purposes
            if (strategy instanceof AmazonScraperStrategy) {
                result.put("expectedPrice", "₹1,449.00");
                result.put("isCorrectPrice", price.isPresent() && 
                    price.get().toString().equals("1449.00"));
            }
            
            return ApiResponse.success(result, "Power adapter scraping test completed");
            
        } catch (Exception e) {
            log.error("Error testing power adapter scraping: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
            return ApiResponse.error("Error testing power adapter: " + e.getMessage());
        }
    }

    /**
     * Test endpoint specifically for diagnosing character encoding issues with the Rupee symbol
     * 
     * @param url the Amazon India URL to test for encoding issues
     * @return detailed information about encoding handling
     */
    @GetMapping("/test-rupee-encoding")
    public ApiResponse<Map<String, Object>> testRupeeEncoding(@RequestParam String url) {
        Map<String, Object> result = new HashMap<>();
        result.put("url", url);
        
        try {
            // Fetch the document
            Document doc = scraperService.fetchDocument(url);
            result.put("fetchSuccess", true);
            
            // Check if this is an Amazon URL
            if (!amazonScraperStrategy.canHandle(url)) {
                return ApiResponse.error("Not an Amazon URL", 400);
            }
            
            // Collect all text containing rupee symbols or potential encoding issues
            Elements elements = doc.select("*:containsOwn(₹), *:containsOwn(Γé╣), *:containsOwn(Rs.)");
            
            List<Map<String, Object>> priceTexts = new ArrayList<>();
            for (Element element : elements) {
                String rawText = element.ownText().trim();
                
                // Skip if text is too long or empty
                if (rawText.isEmpty() || rawText.length() > MAX_PRICE_TEXT_LENGTH) {
                    continue;
                }
                
                // Process each text with price-like content
                if (rawText.matches(".*[₹Γé╣Rs\\d,\\.]+.*")) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("rawText", rawText);
                    entry.put("bytesHex", bytesToHex(rawText.getBytes()));
                    
                    // Clean the text to see if it fixes encoding
                    String cleanedText = rawText
                            .replaceAll("Γé╣", "₹")
                            .replaceAll("[^\\p{ASCII}₹]", "");
                    entry.put("cleanedText", cleanedText);
                    
                    // Try to extract price from both raw and cleaned
                    Optional<BigDecimal> rawPrice = amazonScraperStrategy.extractPrice(Jsoup.parse("<div>" + rawText + "</div>"));
                    Optional<BigDecimal> cleanedPrice = amazonScraperStrategy.extractPrice(Jsoup.parse("<div>" + cleanedText + "</div>"));
                    
                    entry.put("rawPriceExtracted", rawPrice.isPresent());
                    rawPrice.ifPresent(p -> entry.put("rawPrice", p));
                    
                    entry.put("cleanedPriceExtracted", cleanedPrice.isPresent());
                    cleanedPrice.ifPresent(p -> entry.put("cleanedPrice", p));
                    
                    // Add to our collection
                    priceTexts.add(entry);
                }
            }
            
            result.put("priceTextsFound", priceTexts.size());
            result.put("priceTexts", priceTexts);
            
            // Try to extract price with our improved method
            Optional<BigDecimal> extractedPrice = amazonScraperStrategy.extractPrice(doc);
            result.put("extractedPrice", extractedPrice.orElse(null));
            
            return ApiResponse.success(result, "Rupee encoding diagnostics completed");
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ApiResponse.error("Error testing Rupee encoding: " + e.getMessage(), 500);
        }
    }
    
    /**
     * Helper method to convert bytes to hex string for debugging
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Simple endpoint to directly test parsing of corrupted Rupee symbol text
     * 
     * @param priceText The text containing the price with potential encoding issues
     * @return Parsing result
     */
    @GetMapping("/parse-price")
    public ApiResponse<Map<String, Object>> testPriceParsing(@RequestParam String priceText) {
        Map<String, Object> result = new HashMap<>();
        result.put("inputText", priceText);
        
        try {
            // Check if the text contains the corrupted Rupee symbol
            boolean containsCorruptedSymbol = priceText.contains("Γé╣");
            result.put("containsCorruptedSymbol", containsCorruptedSymbol);
            
            // Direct test of AmazonScraperStrategy's price parsing
            Optional<BigDecimal> parsedPrice = amazonScraperStrategy.extractPrice(
                Jsoup.parse("<div>" + priceText + "</div>")
            );
            
            result.put("successfullyParsed", parsedPrice.isPresent());
            parsedPrice.ifPresent(p -> result.put("parsedPrice", p.toString()));
            
            // Test our specific tryParsePrice method directly using reflection for diagnosis
            try {
                java.lang.reflect.Method tryParsePriceMethod = 
                    AmazonScraperStrategy.class.getDeclaredMethod("tryParsePrice", String.class);
                tryParsePriceMethod.setAccessible(true);
                
                @SuppressWarnings("unchecked")
                Optional<BigDecimal> directResult = 
                    (Optional<BigDecimal>) tryParsePriceMethod.invoke(amazonScraperStrategy, priceText);
                
                result.put("directMethodSuccessful", directResult.isPresent());
                directResult.ifPresent(p -> result.put("directMethodResult", p.toString()));
            } catch (Exception e) {
                result.put("reflectionError", e.getMessage());
            }
            
            // Try some low-level diagnostics - hex representation of the text
            StringBuilder hexString = new StringBuilder();
            for (byte b : priceText.getBytes()) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            result.put("hexRepresentation", hexString.toString());
            
            // Try a really basic approach - extract just digits, commas and dots
            String simpleExtract = priceText.replaceAll("[^0-9,.]", "");
            result.put("strippedToDigits", simpleExtract);
            
            try {
                // Try to parse after removing commas
                BigDecimal simplePrice = new BigDecimal(simpleExtract.replace(",", ""));
                result.put("simpleParseResult", simplePrice.toString());
            } catch (Exception e) {
                result.put("simpleParseError", e.getMessage());
            }
            
            return ApiResponse.success(result, "Price parsing test completed");
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ApiResponse.error("Error testing price parsing: " + e.getMessage(), 500);
        }
    }
} 