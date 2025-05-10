package com.pricetracker.app.controller;

import com.pricetracker.app.dto.response.ApiResponse;
import com.pricetracker.app.scraping.ProductDetails;
import com.pricetracker.app.scraping.ScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
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
} 