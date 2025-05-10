package com.pricetracker.app.scheduling;

import com.pricetracker.app.entity.PriceHistory;
import com.pricetracker.app.entity.Product;
import com.pricetracker.app.entity.TrackedProduct;
import com.pricetracker.app.notification.NotificationService;
import com.pricetracker.app.repository.PriceHistoryRepository;
import com.pricetracker.app.repository.ProductRepository;
import com.pricetracker.app.repository.TrackedProductRepository;
import com.pricetracker.app.scraping.ScraperService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PriceCheckScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(PriceCheckScheduler.class);
    
    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final TrackedProductRepository trackedProductRepository;
    private final ScraperService scraperService;
    private final NotificationService notificationService;
    
    @Value("${app.scraper.default-delay-ms:1000}")
    private long defaultDelayMs;
    
    @Value("${app.notification.cooldown-hours:24}")
    private long notificationCooldownHours;
    
    @Scheduled(fixedRateString = "${app.scheduling.checkRateMs:30000}")
    @Transactional
    public void checkPrices() {
        log.info("Starting scheduled price check");
        List<Product> products = productRepository.findAll();
        
        for (Product product : products) {
            try {
                checkProductPrice(product);
                Thread.sleep(defaultDelayMs); // Add delay between requests
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Price check interrupted", e);
                break;
            } catch (Exception e) {
                log.error("Error checking price for product {}: {}", product.getId(), e.getMessage());
            }
        }
        
        log.info("Completed scheduled price check");
    }
    
    private void checkProductPrice(Product product) {
        log.debug("Checking price for product: {}", product.getProductUrl());
        
        scraperService.scrapePrice(product.getProductUrl())
            .ifPresent(scrapedPrice -> {
                if (isPriceChanged(product.getLastCheckedPrice(), scrapedPrice)) {
                    updateProductPrice(product, scrapedPrice);
                    
                    // Check if any notifications need to be sent
                    if (isPriceDrop(product.getLastCheckedPrice(), scrapedPrice)) {
                        log.debug("Price drop detected for product {}: {} -> {}", 
                            product.getId(), product.getLastCheckedPrice(), scrapedPrice);
                        sendNotifications(product, scrapedPrice);
                    } else {
                        log.debug("Price changed but not a drop for product {}, no notifications needed", product.getId());
                    }
                }
            });
    }
    
    private boolean isPriceChanged(BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice == null || newPrice == null) {
            return true;
        }
        return oldPrice.compareTo(newPrice) != 0;
    }
    
    private boolean isPriceDrop(BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice == null || newPrice == null) {
            return false;
        }
        return newPrice.compareTo(oldPrice) < 0;
    }
    
    private void updateProductPrice(Product product, BigDecimal newPrice) {
        log.info("Price changed for product {}: {} -> {}", 
            product.getId(), product.getLastCheckedPrice(), newPrice);
        
        // Update product's last checked price
        product.setLastCheckedPrice(newPrice);
        productRepository.save(product);
        
        // Create price history record
        PriceHistory priceHistory = new PriceHistory();
        priceHistory.setProduct(product);
        priceHistory.setPrice(newPrice);
        priceHistory.setTimestamp(Instant.now());
        priceHistoryRepository.save(priceHistory);
    }
    
    private void sendNotifications(Product product, BigDecimal currentPrice) {
        log.info("Looking for users to notify about price drop for product {}", product.getId());
        
        List<TrackedProduct> trackedProducts = trackedProductRepository
            .findByProductIdAndNotificationEnabledTrue(product.getId());
        
        log.debug("Found {} tracked products with notifications enabled for product {}", 
            trackedProducts.size(), product.getId());
        
        Instant now = Instant.now();
        Duration cooldownDuration = Duration.ofHours(notificationCooldownHours);
        
        for (TrackedProduct trackedProduct : trackedProducts) {
            log.debug("Checking tracked product ID: {}, Desired price: {}, Current price: {}", 
                trackedProduct.getId(), trackedProduct.getDesiredPrice(), currentPrice);
                
            if (currentPrice.compareTo(trackedProduct.getDesiredPrice()) <= 0) {
                log.debug("Price {} is below or equal to desired price {} for tracked product {}", 
                    currentPrice, trackedProduct.getDesiredPrice(), trackedProduct.getId());
                
                // Check if we should send a notification (based on cooldown)
                boolean shouldNotify = trackedProduct.getLastNotifiedAt() == null || 
                    Duration.between(trackedProduct.getLastNotifiedAt(), now).compareTo(cooldownDuration) > 0;
                
                log.debug("Cooldown check for tracked product {}: lastNotifiedAt={}, shouldNotify={}", 
                    trackedProduct.getId(), trackedProduct.getLastNotifiedAt(), shouldNotify);
                
                if (shouldNotify) {
                    log.debug("Sending price drop notification to user {} for product {}", 
                        trackedProduct.getUserId(), product.getId());
                    
                    notificationService.sendPriceAlert(trackedProduct, currentPrice);
                    trackedProduct.setLastNotifiedAt(now);
                    trackedProductRepository.save(trackedProduct);
                } else {
                    log.debug("Skipping notification for user {} for product {} (cooldown period active)", 
                        trackedProduct.getUserId(), product.getId());
                }
            } else {
                log.debug("Price {} is NOT below desired price {} for tracked product {}, no notification needed", 
                    currentPrice, trackedProduct.getDesiredPrice(), trackedProduct.getId());
            }
        }
    }
} 