package com.pricetracker.app.scheduling;

import com.pricetracker.app.entity.PriceHistory;
import com.pricetracker.app.entity.Product;
import com.pricetracker.app.entity.TrackedProduct;
import com.pricetracker.app.notification.NotificationService;
import com.pricetracker.app.repository.PriceHistoryRepository;
import com.pricetracker.app.repository.ProductRepository;
import com.pricetracker.app.repository.TrackedProductRepository;
import com.pricetracker.app.scraping.ScraperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceCheckSchedulerTest {

    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private PriceHistoryRepository priceHistoryRepository;
    
    @Mock
    private TrackedProductRepository trackedProductRepository;
    
    @Mock
    private ScraperService scraperService;
    
    @Mock
    private NotificationService notificationService;
    
    @InjectMocks
    private PriceCheckScheduler priceCheckScheduler;
    
    private Product testProduct1;
    private Product testProduct2;
    private TrackedProduct trackedProduct;
    
    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(priceCheckScheduler, "defaultDelayMs", 0L);
        ReflectionTestUtils.setField(priceCheckScheduler, "notificationCooldownHours", 24L);
        
        testProduct1 = new Product();
        testProduct1.setId(1L);
        testProduct1.setProductUrl("https://example.com/product1");
        testProduct1.setLastCheckedPrice(new BigDecimal("99.99"));
        
        testProduct2 = new Product();
        testProduct2.setId(2L);
        testProduct2.setProductUrl("https://example.com/product2");
        testProduct2.setLastCheckedPrice(new BigDecimal("149.99"));
        
        trackedProduct = new TrackedProduct();
        trackedProduct.setId(1L);
        trackedProduct.setUserId(1L);
        trackedProduct.setProduct(testProduct1);
        trackedProduct.setDesiredPrice(new BigDecimal("90.00"));
        trackedProduct.setNotificationEnabled(true);
        // No last notification, should send
        trackedProduct.setLastNotifiedAt(null);
    }
    
    @Test
    void whenCheckPrices_withPriceChange_thenUpdatePriceAndHistory() {
        // Given
        when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct1, testProduct2));
        when(scraperService.scrapePrice(testProduct1.getProductUrl()))
            .thenReturn(Optional.of(new BigDecimal("89.99")));
        when(scraperService.scrapePrice(testProduct2.getProductUrl()))
            .thenReturn(Optional.of(new BigDecimal("149.99")));
        
        // For notification test
        when(trackedProductRepository.findByProductIdAndNotificationEnabledTrue(testProduct1.getId()))
            .thenReturn(Collections.singletonList(trackedProduct));
        
        // When
        priceCheckScheduler.checkPrices();
        
        // Then
        verify(productRepository).save(testProduct1);
        verify(priceHistoryRepository).save(any(PriceHistory.class));
        verify(productRepository, never()).save(testProduct2);
        verify(notificationService).sendPriceAlert(eq(trackedProduct), any(BigDecimal.class));
        verify(trackedProductRepository).save(trackedProduct);
    }
    
    @Test
    void whenCheckPrices_withCooldownActive_thenDoNotSendNotification() {
        // Given
        // Set a recent last notification time (within cooldown)
        trackedProduct.setLastNotifiedAt(Instant.now().minusSeconds(3600)); // 1 hour ago
        
        when(productRepository.findAll()).thenReturn(Collections.singletonList(testProduct1));
        when(scraperService.scrapePrice(testProduct1.getProductUrl()))
            .thenReturn(Optional.of(new BigDecimal("89.99")));
        
        // For notification test
        when(trackedProductRepository.findByProductIdAndNotificationEnabledTrue(testProduct1.getId()))
            .thenReturn(Collections.singletonList(trackedProduct));
        
        // When
        priceCheckScheduler.checkPrices();
        
        // Then
        verify(productRepository).save(testProduct1);
        verify(priceHistoryRepository).save(any(PriceHistory.class));
        verify(notificationService, never()).sendPriceAlert(any(TrackedProduct.class), any(BigDecimal.class));
        // Verify trackedProduct was NOT updated
        verify(trackedProductRepository, never()).save(trackedProduct);
    }
    
    @Test
    void whenCheckPrices_withCooldownExpired_thenSendNotification() {
        // Given
        // Set an old last notification time (outside cooldown)
        trackedProduct.setLastNotifiedAt(Instant.now().minusSeconds(86400 * 2)); // 2 days ago
        
        when(productRepository.findAll()).thenReturn(Collections.singletonList(testProduct1));
        when(scraperService.scrapePrice(testProduct1.getProductUrl()))
            .thenReturn(Optional.of(new BigDecimal("89.99")));
        
        // For notification test
        when(trackedProductRepository.findByProductIdAndNotificationEnabledTrue(testProduct1.getId()))
            .thenReturn(Collections.singletonList(trackedProduct));
        
        // When
        priceCheckScheduler.checkPrices();
        
        // Then
        verify(productRepository).save(testProduct1);
        verify(priceHistoryRepository).save(any(PriceHistory.class));
        verify(notificationService).sendPriceAlert(eq(trackedProduct), any(BigDecimal.class));
        verify(trackedProductRepository).save(trackedProduct);
    }
    
    @Test
    void whenCheckPrices_withScrapingError_thenContinueWithNextProduct() {
        // Given
        when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct1, testProduct2));
        when(scraperService.scrapePrice(testProduct1.getProductUrl()))
            .thenThrow(new RuntimeException("Scraping failed"));
        when(scraperService.scrapePrice(testProduct2.getProductUrl()))
            .thenReturn(Optional.of(new BigDecimal("149.99")));
        
        // When
        priceCheckScheduler.checkPrices();
        
        // Then
        verify(productRepository, never()).save(testProduct1);
        verify(priceHistoryRepository, never()).save(any(PriceHistory.class));
        verify(productRepository, never()).save(testProduct2);
        verify(notificationService, never()).sendPriceAlert(any(TrackedProduct.class), any(BigDecimal.class));
    }
    
    @Test
    void whenCheckPrices_withNoPriceChange_thenDoNotUpdate() {
        // Given
        when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct1));
        when(scraperService.scrapePrice(testProduct1.getProductUrl()))
            .thenReturn(Optional.of(testProduct1.getLastCheckedPrice()));
        
        // When
        priceCheckScheduler.checkPrices();
        
        // Then
        verify(productRepository, never()).save(any(Product.class));
        verify(priceHistoryRepository, never()).save(any(PriceHistory.class));
        verify(notificationService, never()).sendPriceAlert(any(TrackedProduct.class), any(BigDecimal.class));
    }
} 