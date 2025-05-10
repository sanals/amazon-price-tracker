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
            // Try a variety of common price selectors used by popular e-commerce sites
            Element priceElement = doc.selectFirst(
                // General selectors
                "#price, .price, [data-price], .product-price, .current-price, .price-current, " +
                // Amazon selectors
                "#priceblock_ourprice, #priceblock_dealprice, .a-price .a-offscreen, .a-price-whole, " +
                // Walmart selectors 
                ".price-characteristic, [data-automation-id='product-price'], " +
                // Best Buy selectors
                ".priceView-customer-price span, .pb-purchase-price, " +
                // eBay selectors
                ".x-price-primary, .display-price, " +
                // New Egg selectors
                ".price-current, " +
                // Target selectors
                ".style__PriceFontSize-sc-__sc-17a8wpr-0, [data-test='product-price']"
            );
            
            if (priceElement == null) {
                log.debug("No price element found using primary selectors, trying secondary approach");
                // Try a different approach - find ALL elements containing currency symbols or patterns
                for (Element element : doc.select("*:containsOwn($), *:containsOwn(€), *:containsOwn(£), " +
                        "*:containsOwn(¥), *:containsOwn(₹), *:containsOwn(price), *:containsOwn(Price)")) {
                    String text = element.text().trim();
                    // Check if it has digits and common price patterns
                    if (text.matches(".*\\d+.*") && 
                        (text.contains("$") || text.contains("€") || text.contains("£") || 
                         text.contains("¥") || text.contains("₹") || 
                         text.toLowerCase().contains("price"))) {
                        priceElement = element;
                        break;
                    }
                }
            }
            
            if (priceElement == null) {
                log.debug("No price element found in document");
                return Optional.empty();
            }
            
            String priceText = priceElement.text()
                .replaceAll("[^\\d.,]", "") // Remove non-numeric characters except . and ,
                .replace(",", "."); // Normalize decimal separator
            
            // Additional handling for multiple dots (e.g., 1.234.56)
            if (priceText.indexOf('.') != priceText.lastIndexOf('.')) {
                priceText = priceText.replaceAll("\\.(?=.*\\.)", "");
            }
            
            return Optional.of(new BigDecimal(priceText));
        } catch (Exception e) {
            log.debug("Failed to extract price from document: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    private Optional<String> extractName(Document doc) {
        try {
            // Try a variety of common name/title selectors used by popular e-commerce sites
            Element nameElement = doc.selectFirst(
                // Common selectors
                "h1, #productTitle, .product-title, .product-name, .title, " +
                // Amazon selectors
                "#productTitle, #title, " +
                // Walmart selectors
                "[data-automation-id='product-title'], .prod-ProductTitle, " +
                // Best Buy selectors
                ".heading-5.v-fw-regular, .sku-title, " +
                // eBay selectors
                ".x-item-title, .x-item-title__mainTitle, " +
                // NewEgg selectors
                ".product-title, " +
                // Target selectors
                "[data-test='product-title']"
            );
            
            if (nameElement == null) {
                // Try a different approach - often the first h1 on the page is the product title
                nameElement = doc.selectFirst("h1");
                if (nameElement == null) {
                    log.debug("No name element found in document");
                    return Optional.empty();
                }
            }
            
            String name = nameElement.text().trim();
            return Optional.of(name);
        } catch (Exception e) {
            log.debug("Failed to extract name from document: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    private Optional<String> extractImageUrl(Document doc) {
        try {
            // Try a variety of common image selectors used by popular e-commerce sites
            Element imageElement = doc.selectFirst(
                // Common selectors
                "#main-image, .main-image, .product-image, .primary-image, " +
                // Amazon selectors
                "#landingImage, #imgBlkFront, .a-dynamic-image, " +
                // Walmart selectors
                "[data-automation-id='image'], .prod-hero-image-carousel, " +
                // Best Buy selectors
                ".product-image img, .primary-image, " +
                // eBay selectors
                ".img.imgWr2, #icImg, " +
                // NewEgg selectors
                ".product-view-img-original, " +
                // Target selectors
                "[data-test='product-image']"
            );
            
            if (imageElement == null) {
                // Try different approach - look for large images
                for (Element img : doc.select("img")) {
                    if (img.hasAttr("src") && 
                        !img.attr("src").isEmpty() && 
                        (img.hasAttr("width") && Integer.parseInt(img.attr("width")) > 200 || 
                         img.hasAttr("height") && Integer.parseInt(img.attr("height")) > 200)) {
                        imageElement = img;
                        break;
                    }
                }
                
                // If still not found, just get the first meaningful image
                if (imageElement == null) {
                    for (Element img : doc.select("img")) {
                        if (img.hasAttr("src") && 
                            !img.attr("src").isEmpty() && 
                            !img.attr("src").endsWith(".gif") &&
                            !img.attr("src").contains("icon") &&
                            !img.attr("src").contains("logo")) {
                            imageElement = img;
                            break;
                        }
                    }
                }
            }
            
            if (imageElement == null) {
                log.debug("No image element found in document");
                return Optional.empty();
            }
            
            // Try different image attributes, prioritize data-a-dynamic-image (JSON) in case of Amazon
            String imageUrl;
            if (imageElement.hasAttr("data-a-dynamic-image")) {
                String jsonImages = imageElement.attr("data-a-dynamic-image");
                imageUrl = jsonImages.substring(2, jsonImages.indexOf('"', 2)); // Extract first URL
            } else if (imageElement.hasAttr("data-old-hires")) {
                imageUrl = imageElement.attr("data-old-hires");
            } else if (imageElement.hasAttr("srcset")) {
                String srcset = imageElement.attr("srcset");
                imageUrl = srcset.split("\\s+")[0]; // First URL in srcset
            } else {
                imageUrl = imageElement.attr("src");
            }
            
            // Make sure we have an absolute URL
            if (imageUrl.startsWith("//")) {
                imageUrl = "https:" + imageUrl;
            } else if (imageUrl.startsWith("/")) {
                // Extract domain from document baseUri to create absolute URL
                String baseUrl = doc.baseUri();
                try {
                    java.net.URL url = new java.net.URL(baseUrl);
                    imageUrl = url.getProtocol() + "://" + url.getHost() + imageUrl;
                } catch (Exception e) {
                    log.debug("Failed to parse base URL, using relative image URL");
                }
            }
            
            return Optional.of(imageUrl);
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
        } else {
            log.warn("Error while scraping {} from URL: {}", type, url, e);
        }
    }
} 