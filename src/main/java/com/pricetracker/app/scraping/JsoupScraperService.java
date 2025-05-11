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
import java.net.URI;
import java.net.UnknownHostException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Implementation of ScraperService using Jsoup library.
 */
@Service
public class JsoupScraperService implements ScraperService {
    
    private static final Logger log = LoggerFactory.getLogger(JsoupScraperService.class);
    private static final Random random = new Random();
    
    // Common browser user agents for rotation
    private static final String[] BROWSER_USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1.2 Safari/605.1.15",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:90.0) Gecko/20100101 Firefox/90.0"
    };
    
    @Value("${app.scraper.user-agent}")
    private String configuredUserAgent;
    
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
        // Expand shortened URLs (like amzn.in) first
        if (url.contains("amzn.in") || url.contains("a.co")) {
            log.debug("Detected shortened Amazon URL: {}, expanding it", url);
            url = expandShortenedUrl(url);
            log.debug("Expanded URL: {}", url);
        }
        
        // Add random delay to avoid detection (between 1-3 seconds)
        try {
            Thread.sleep(1000 + random.nextInt(2000));
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
            
            return Jsoup.connect(url)
                .userAgent(userAgent)
                .headers(headers)
                .timeout(15000) // Extended timeout (15 seconds)
                .followRedirects(true)
                .maxBodySize(0) // Unlimited body size
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
    
    /**
     * Expand a shortened URL to its full form
     */
    private String expandShortenedUrl(String shortUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URI(shortUrl).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", getRandomUserAgent());
            connection.setRequestMethod("HEAD");
            
            int responseCode = connection.getResponseCode();
            if (responseCode >= 300 && responseCode < 400) {
                String expandedUrl = connection.getHeaderField("Location");
                if (expandedUrl != null && !expandedUrl.isEmpty()) {
                    log.debug("Expanded URL {} to {}", shortUrl, expandedUrl);
                    // If still a relative URL, handle it
                    if (expandedUrl.startsWith("/")) {
                        expandedUrl = url.getProtocol() + "://" + url.getHost() + expandedUrl;
                    }
                    return expandedUrl;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to expand shortened URL {}: {}", shortUrl, e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return shortUrl; // Return original if expansion fails
    }
    
    private String getRandomUserAgent() {
        // Use a random browser-like user agent instead of the bot identifier
        return BROWSER_USER_AGENTS[random.nextInt(BROWSER_USER_AGENTS.length)];
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
                    URL url = new URI(baseUrl).toURL();
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