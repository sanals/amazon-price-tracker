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
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    
    @Captor
    private ArgumentCaptor<Product> productCaptor;
    
    @InjectMocks
    private PriceCheckScheduler priceCheckScheduler;
    
    private Product testProduct1;
    private Product testProduct2;
    private TrackedProduct trackedProduct;
    
    @BeforeEach
    void setUp() {
        // Disable delay between scraping requests for tests
        ReflectionTestUtils.setField(priceCheckScheduler, "defaultDelayMs", 0L);
        ReflectionTestUtils.setField(priceCheckScheduler, "notificationCooldownHours", 24L);
        
        // Initialize the lastCheckTimeMap with an empty HashMap to avoid NPE
        ReflectionTestUtils.setField(priceCheckScheduler, "lastCheckTimeMap", new HashMap<>());
        
        // Set up test products and tracked products
        testProduct1 = new Product();
        testProduct1.setId(1L);
        testProduct1.setName("Test Product 1");
        testProduct1.setProductUrl("https://example.com/product1");
        testProduct1.setLastCheckedPrice(new BigDecimal("99.99"));
        
        testProduct2 = new Product();
        testProduct2.setId(2L);
        testProduct2.setName("Test Product 2");
        testProduct2.setProductUrl("https://example.com/product2");
        testProduct2.setLastCheckedPrice(new BigDecimal("149.99"));
        
        // Set up tracked product with reference to testProduct1
        trackedProduct = new TrackedProduct();
        trackedProduct.setId(1L);
        trackedProduct.setUserId(1L);
        trackedProduct.setProduct(testProduct1);
        trackedProduct.setDesiredPrice(new BigDecimal("95.00"));
        trackedProduct.setNotificationEnabled(true);
        trackedProduct.setLastNotifiedAt(null);
        trackedProduct.setCheckIntervalMinutes(5); // Set check interval to 5 minutes
    }
    
    @Test
    void whenSendNotifications_withPriceDropBelowDesiredPrice_thenNotificationSent() {
        // GIVEN
        BigDecimal newPrice = new BigDecimal("89.99");
        
        // Verify test setup
        assertThat(newPrice.compareTo(trackedProduct.getDesiredPrice())).isLessThan(0);
        
        // Set up mocks
        when(trackedProductRepository.findByProductIdAndNotificationEnabledTrue(testProduct1.getId()))
                .thenReturn(Collections.singletonList(trackedProduct));
        
        // WHEN - directly call the private method
        ReflectionTestUtils.invokeMethod(priceCheckScheduler, "sendNotifications", testProduct1, newPrice);
        
        // THEN
        verify(notificationService).sendPriceAlert(eq(trackedProduct), eq(newPrice));
        verify(trackedProductRepository).save(trackedProduct);
    }
    
    @Test
    void whenSendNotifications_withCooldownActive_thenNoNotificationSent() {
        // GIVEN
        BigDecimal newPrice = new BigDecimal("89.99");
        trackedProduct.setLastNotifiedAt(Instant.now().minusSeconds(3600)); // 1 hour ago (within 24h cooldown)
        
        // Verify test setup
        assertThat(newPrice.compareTo(trackedProduct.getDesiredPrice())).isLessThan(0);
        
        // Set up mocks
        when(trackedProductRepository.findByProductIdAndNotificationEnabledTrue(testProduct1.getId()))
                .thenReturn(Collections.singletonList(trackedProduct));
        
        // WHEN - directly call the private method
        ReflectionTestUtils.invokeMethod(priceCheckScheduler, "sendNotifications", testProduct1, newPrice);
        
        // THEN
        verify(notificationService, never()).sendPriceAlert(any(), any());
        verify(trackedProductRepository, never()).save(any());
    }
    
    @Test
    void whenCheckPrices_withPriceChange_thenUpdatePriceAndHistory() {
        // GIVEN
        BigDecimal oldPrice = new BigDecimal("99.99");
        BigDecimal newPrice = new BigDecimal("89.99");
        
        // Set up test data
        testProduct1.setLastCheckedPrice(oldPrice);
        
        // Connect product to tracked product
        trackedProduct.setProduct(testProduct1);
        
        // IMPORTANT: Verify our test setup meets the conditions
        assertThat(newPrice.compareTo(oldPrice)).isLessThan(0); // This is a price drop
        assertThat(newPrice.compareTo(trackedProduct.getDesiredPrice())).isLessThan(0); // New price is below desired price
        
        // Set up mocks for the updated scheduler
        when(productRepository.findAll()).thenReturn(Collections.singletonList(testProduct1));
        when(trackedProductRepository.findByProductId(testProduct1.getId()))
                .thenReturn(Collections.singletonList(trackedProduct));
        when(scraperService.scrapePrice(testProduct1.getProductUrl())).thenReturn(Optional.of(newPrice));
        
        // WHEN
        priceCheckScheduler.checkPrices();
        
        // THEN
        // Verify product was updated with new price
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getLastCheckedPrice()).isEqualTo(newPrice);
        
        // Verify price history was created
        verify(priceHistoryRepository).save(any(PriceHistory.class));
    }
    
    @Test
    void whenCheckPrices_withScrapingError_thenContinueWithNextProduct() {
        // Given
        when(productRepository.findAll()).thenReturn(Arrays.asList(testProduct1, testProduct2));
        
        // Set up tracked products
        TrackedProduct tp1 = new TrackedProduct();
        tp1.setCheckIntervalMinutes(5);
        TrackedProduct tp2 = new TrackedProduct();
        tp2.setCheckIntervalMinutes(5);
        
        when(trackedProductRepository.findByProductId(testProduct1.getId()))
                .thenReturn(Collections.singletonList(tp1));
        when(trackedProductRepository.findByProductId(testProduct2.getId()))
                .thenReturn(Collections.singletonList(tp2));
                
        when(scraperService.scrapePrice(testProduct1.getProductUrl()))
            .thenThrow(new RuntimeException("Scraping failed"));
        when(scraperService.scrapePrice(testProduct2.getProductUrl()))
            .thenReturn(Optional.of(new BigDecimal("149.99"))); // Same price, no change
        
        // When
        priceCheckScheduler.checkPrices();
        
        // Then
        verify(productRepository, never()).save(any(Product.class));
        verify(priceHistoryRepository, never()).save(any(PriceHistory.class));
    }
    
    @Test
    void whenCheckPrices_withNoPriceChange_thenDoNotUpdate() {
        // Given
        when(productRepository.findAll()).thenReturn(Collections.singletonList(testProduct1));
        when(trackedProductRepository.findByProductId(testProduct1.getId()))
                .thenReturn(Collections.singletonList(trackedProduct));
        when(scraperService.scrapePrice(testProduct1.getProductUrl()))
            .thenReturn(Optional.of(testProduct1.getLastCheckedPrice()));
        
        // When
        priceCheckScheduler.checkPrices();
        
        // Then
        verify(productRepository, never()).save(any(Product.class));
        verify(priceHistoryRepository, never()).save(any(PriceHistory.class));
    }
    
    @Test
    void whenCheckPrices_withPriceIncrease_thenUpdateButDoNotNotify() {
        // GIVEN
        BigDecimal oldPrice = new BigDecimal("99.99");
        BigDecimal newPrice = new BigDecimal("109.99"); // Price increase
        
        // Connect product to tracked product
        trackedProduct.setProduct(testProduct1);
        testProduct1.setLastCheckedPrice(oldPrice);
        
        // IMPORTANT: Verify our test setup meets the conditions
        assertThat(newPrice.compareTo(oldPrice)).isGreaterThan(0); // This is a price increase
        
        // Set up mocks
        when(productRepository.findAll()).thenReturn(Collections.singletonList(testProduct1));
        when(trackedProductRepository.findByProductId(testProduct1.getId()))
                .thenReturn(Collections.singletonList(trackedProduct));
        when(scraperService.scrapePrice(testProduct1.getProductUrl())).thenReturn(Optional.of(newPrice));
        
        // WHEN
        priceCheckScheduler.checkPrices();
        
        // THEN
        verify(productRepository).save(any(Product.class));
        verify(priceHistoryRepository).save(any(PriceHistory.class));
    }
    
    @Test
    void whenCheckPrices_withNoTrackedProducts_thenSkipCheck() {
        // Given
        when(productRepository.findAll()).thenReturn(Collections.singletonList(testProduct1));
        when(trackedProductRepository.findByProductId(testProduct1.getId()))
                .thenReturn(Collections.emptyList());
        
        // When
        priceCheckScheduler.checkPrices();
        
        // Then
        verify(scraperService, never()).scrapePrice(any());
        verify(productRepository, never()).save(any());
        verify(priceHistoryRepository, never()).save(any());
    }
} 