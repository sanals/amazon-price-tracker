package com.pricetracker.app.service;

import com.pricetracker.app.dto.request.TrackProductRequest;
import com.pricetracker.app.dto.request.UpdateTrackedProductRequest;
import com.pricetracker.app.entity.Product;
import com.pricetracker.app.entity.TrackedProduct;
import com.pricetracker.app.exception.ProductAlreadyTrackedException;
import com.pricetracker.app.exception.ResourceNotFoundException;
import com.pricetracker.app.repository.ProductRepository;
import com.pricetracker.app.repository.TrackedProductRepository;
import com.pricetracker.app.scraping.ProductDetails;
import com.pricetracker.app.scraping.ScraperService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductTrackingServiceTest {

    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private TrackedProductRepository trackedProductRepository;
    
    @Mock
    private ScraperService scraperService;
    
    @InjectMocks
    private ProductTrackingService productTrackingService;
    
    private static final Long USER_ID = 1L;
    private static final String PRODUCT_URL = "https://example.com/product";
    private static final BigDecimal DESIRED_PRICE = new BigDecimal("99.99");
    
    private Product testProduct;
    private TrackedProduct testTrackedProduct;
    private ProductDetails testProductDetails;
    
    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setProductUrl(PRODUCT_URL);
        testProduct.setName("Test Product");
        testProduct.setLastCheckedPrice(new BigDecimal("149.99"));
        
        testTrackedProduct = new TrackedProduct();
        testTrackedProduct.setId(1L);
        testTrackedProduct.setUserId(USER_ID);
        testTrackedProduct.setProduct(testProduct);
        testTrackedProduct.setDesiredPrice(DESIRED_PRICE);
        testTrackedProduct.setNotificationEnabled(true);
        
        testProductDetails = new ProductDetails(
            Optional.of("Test Product"),
            Optional.of("https://example.com/image.jpg"),
            Optional.of(new BigDecimal("149.99"))
        );
    }
    
    @Test
    void whenAddProductTracking_withNewProduct_thenCreateProductAndTracking() {
        // Given
        TrackProductRequest request = new TrackProductRequest(PRODUCT_URL, DESIRED_PRICE, 60);
        
        when(productRepository.findByProductUrl(PRODUCT_URL)).thenReturn(Optional.empty());
        when(scraperService.scrapeProductDetails(PRODUCT_URL)).thenReturn(Optional.of(testProductDetails));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);
        when(trackedProductRepository.save(any(TrackedProduct.class))).thenReturn(testTrackedProduct);
        
        // When
        TrackedProduct result = productTrackingService.addProductTracking(USER_ID, request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getProduct().getProductUrl()).isEqualTo(PRODUCT_URL);
        assertThat(result.getDesiredPrice()).isEqualTo(DESIRED_PRICE);
        
        verify(productRepository).findByProductUrl(PRODUCT_URL);
        verify(scraperService).scrapeProductDetails(PRODUCT_URL);
        verify(productRepository).save(any(Product.class));
        verify(trackedProductRepository).save(any(TrackedProduct.class));
    }
    
    @Test
    void whenAddProductTracking_withExistingProduct_thenOnlyCreateTracking() {
        // Given
        TrackProductRequest request = new TrackProductRequest(PRODUCT_URL, DESIRED_PRICE, 60);
        
        when(productRepository.findByProductUrl(PRODUCT_URL)).thenReturn(Optional.of(testProduct));
        when(trackedProductRepository.existsByUserIdAndProductId(USER_ID, testProduct.getId())).thenReturn(false);
        when(trackedProductRepository.save(any(TrackedProduct.class))).thenReturn(testTrackedProduct);
        
        // When
        TrackedProduct result = productTrackingService.addProductTracking(USER_ID, request);
        
        // Then
        assertThat(result).isNotNull();
        verify(productRepository).findByProductUrl(PRODUCT_URL);
        verify(scraperService, never()).scrapeProductDetails(any());
        verify(trackedProductRepository).save(any(TrackedProduct.class));
    }
    
    @Test
    void whenAddProductTracking_withAlreadyTrackedProduct_thenThrowException() {
        // Given
        TrackProductRequest request = new TrackProductRequest(PRODUCT_URL, DESIRED_PRICE, 60);
        
        when(productRepository.findByProductUrl(PRODUCT_URL)).thenReturn(Optional.of(testProduct));
        when(trackedProductRepository.existsByUserIdAndProductId(USER_ID, testProduct.getId())).thenReturn(true);
        
        // When/Then
        assertThatThrownBy(() -> productTrackingService.addProductTracking(USER_ID, request))
            .isInstanceOf(ProductAlreadyTrackedException.class);
    }
    
    @Test
    void whenGetTrackedProductsForUser_thenReturnPage() {
        // Given
        Pageable pageable = Pageable.unpaged();
        Page<TrackedProduct> expectedPage = new PageImpl<>(List.of(testTrackedProduct));
        
        when(trackedProductRepository.findByUserId(USER_ID, pageable)).thenReturn(expectedPage);
        
        // When
        Page<TrackedProduct> result = productTrackingService.getTrackedProductsForUser(USER_ID, pageable);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(testTrackedProduct);
    }
    
    @Test
    void whenUpdateTrackedProduct_withValidRequest_thenUpdate() {
        // Given
        UpdateTrackedProductRequest request = new UpdateTrackedProductRequest(
            new BigDecimal("89.99"),
            false,
            60
        );
        
        when(trackedProductRepository.findByIdAndUserId(testTrackedProduct.getId(), USER_ID))
            .thenReturn(Optional.of(testTrackedProduct));
        when(trackedProductRepository.save(any(TrackedProduct.class))).thenReturn(testTrackedProduct);
        
        // When
        TrackedProduct result = productTrackingService.updateTrackedProduct(USER_ID, testTrackedProduct.getId(), request);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDesiredPrice()).isEqualTo(new BigDecimal("89.99"));
        assertThat(result.isNotificationEnabled()).isFalse();
    }
    
    @Test
    void whenUpdateTrackedProduct_withNonexistentId_thenThrowException() {
        // Given
        UpdateTrackedProductRequest request = new UpdateTrackedProductRequest(DESIRED_PRICE, true, 60);
        
        when(trackedProductRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> productTrackingService.updateTrackedProduct(USER_ID, 99L, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }
    
    @Test
    void whenDeleteTrackedProduct_withValidId_thenDelete() {
        // Given
        when(trackedProductRepository.findByIdAndUserId(testTrackedProduct.getId(), USER_ID))
            .thenReturn(Optional.of(testTrackedProduct));
        
        // When
        productTrackingService.deleteTrackedProduct(USER_ID, testTrackedProduct.getId());
        
        // Then
        verify(trackedProductRepository).delete(testTrackedProduct);
    }
    
    @Test
    void whenDeleteTrackedProduct_withNonexistentId_thenThrowException() {
        // Given
        when(trackedProductRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());
        
        // When/Then
        assertThatThrownBy(() -> productTrackingService.deleteTrackedProduct(USER_ID, 99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }
} 