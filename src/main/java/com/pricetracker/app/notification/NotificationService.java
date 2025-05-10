package com.pricetracker.app.notification;

import com.pricetracker.app.entity.TrackedProduct;
import java.math.BigDecimal;

/**
 * Service interface for sending notifications about price changes.
 */
public interface NotificationService {
    
    /**
     * Send a price alert notification for a tracked product.
     * 
     * @param trackedProduct the tracked product
     * @param currentPrice the current price of the product
     */
    void sendPriceAlert(TrackedProduct trackedProduct, BigDecimal currentPrice);
} 