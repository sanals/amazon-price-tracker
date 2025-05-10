package com.pricetracker.app.scraping;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.HttpStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * Implementation of ScraperService using Jsoup library.
 */
@Service
public class JsoupScraperService implements ScraperService {
    
    private static final Logger log = LoggerFactory.getLogger(JsoupScraperService.class);
    
    @Value("${app.scraper.user-agent}")
    private String userAgent;
    
    @Override
    public Optional<BigDecimal> scrapePrice(String productUrl) {
        try {
            Document doc = fetchDocument(productUrl);
            return extractPrice(doc);
        } catch (Exception e) {
            logScrapingError("price", productUrl, e);
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<ProductDetails> scrapeProductDetails(String productUrl) {
        try {
            Document doc = fetchDocument(productUrl);
            return Optional.of(new ProductDetails(
                extractName(doc),
                extractImageUrl(doc),
                extractPrice(doc)
            ));
        } catch (Exception e) {
            logScrapingError("product details", productUrl, e);
            return Optional.empty();
        }
    }
    
    private Document fetchDocument(String url) throws IOException {
        try {
            return Jsoup.connect(url)
                .userAgent(userAgent)
                .timeout(10000) // 10 seconds
                .get();
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
    
    private Optional<BigDecimal> extractPrice(Document doc) {
        try {
            Element priceElement = doc.selectFirst("#price, .price, [data-price]"); // Example selectors
            if (priceElement == null) {
                return Optional.empty();
            }
            
            String priceText = priceElement.text()
                .replaceAll("[^\\d.,]", "") // Remove non-numeric characters except . and ,
                .replace(",", "."); // Normalize decimal separator
            
            return Optional.of(new BigDecimal(priceText));
        } catch (Exception e) {
            log.debug("Failed to extract price from document", e);
            return Optional.empty();
        }
    }
    
    private Optional<String> extractName(Document doc) {
        try {
            Element nameElement = doc.selectFirst("#productTitle, .product-name, h1"); // Example selectors
            return Optional.ofNullable(nameElement)
                .map(Element::text)
                .map(String::trim);
        } catch (Exception e) {
            log.debug("Failed to extract name from document", e);
            return Optional.empty();
        }
    }
    
    private Optional<String> extractImageUrl(Document doc) {
        try {
            Element imageElement = doc.selectFirst("#mainImage, .product-image img"); // Example selectors
            return Optional.ofNullable(imageElement)
                .map(element -> element.attr("src"))
                .filter(src -> !src.isEmpty());
        } catch (Exception e) {
            log.debug("Failed to extract image URL from document", e);
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
        } else {
            log.warn("Error while scraping {} from URL: {}", type, url, e);
        }
    }
} 