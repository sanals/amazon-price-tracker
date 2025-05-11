package com.pricetracker.app.controller;

import com.pricetracker.app.dto.response.ApiResponse;
import com.pricetracker.app.entity.PriceHistory;
import com.pricetracker.app.entity.Product;
import com.pricetracker.app.repository.PriceHistoryRepository;
import com.pricetracker.app.repository.ProductRepository;
import com.pricetracker.app.scraping.AmazonScraperStrategy;
import com.pricetracker.app.scraping.ProductDetails;
import com.pricetracker.app.scraping.ScraperService;
import com.pricetracker.app.scheduling.PriceCheckScheduler;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
import java.time.Instant;
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
} 