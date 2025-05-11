package com.pricetracker.app.service;

import com.pricetracker.app.dto.request.TrackProductRequest;
import com.pricetracker.app.dto.request.UpdateTrackedProductRequest;
import com.pricetracker.app.entity.Product;
import com.pricetracker.app.entity.TrackedProduct;
import com.pricetracker.app.exception.ResourceNotFoundException;
import com.pricetracker.app.exception.ProductAlreadyTrackedException;
import com.pricetracker.app.repository.ProductRepository;
import com.pricetracker.app.repository.TrackedProductRepository;
import com.pricetracker.app.scraping.ProductDetails;
import com.pricetracker.app.scraping.ScraperService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductTrackingService {
    
    private final ProductRepository productRepository;
    private final TrackedProductRepository trackedProductRepository;
    private final ScraperService scraperService;
    
    @Transactional
    public TrackedProduct addProductTracking(Long userId, TrackProductRequest request) {
        // First try to find existing product
        Product product = productRepository.findByProductUrl(request.productUrl())
            .orElseGet(() -> createNewProduct(request.productUrl()));
        
        // Check if user already tracks this product
        if (trackedProductRepository.existsByUserIdAndProductId(userId, product.getId())) {
            throw new ProductAlreadyTrackedException("You are already tracking this product");
        }
        
        // Create new tracking
        TrackedProduct trackedProduct = new TrackedProduct();
        trackedProduct.setUserId(userId);
        trackedProduct.setProduct(product);
        trackedProduct.setDesiredPrice(request.desiredPrice());
        trackedProduct.setNotificationEnabled(true); // Default to enabled
        
        return trackedProductRepository.save(trackedProduct);
    }
    
    private Product createNewProduct(String productUrl) {
        // Try to scrape initial product details
        ProductDetails details = scraperService.scrapeProductDetails(productUrl)
            .orElse(ProductDetails.empty());
        
        Product product = new Product();
        product.setProductUrl(productUrl);
        product.setName(details.name().orElse("Unknown Product")); // Fallback name if scraping fails
        product.setImageUrl(details.imageUrl().orElse(null));
        product.setLastCheckedPrice(details.price().orElse(null));
        
        return productRepository.save(product);
    }
    
    @Transactional(readOnly = true)
    public Page<TrackedProduct> getTrackedProductsForUser(Long userId, Pageable pageable) {
        return trackedProductRepository.findByUserId(userId, pageable);
    }
    
    @Transactional(readOnly = true)
    public Optional<TrackedProduct> getTrackedProductById(Long userId, Long trackedProductId) {
        return trackedProductRepository.findByIdAndUserId(trackedProductId, userId);
    }
    
    @Transactional
    public TrackedProduct updateTrackedProduct(Long userId, Long trackedProductId, 
                                             UpdateTrackedProductRequest request) {
        TrackedProduct trackedProduct = getTrackedProductById(userId, trackedProductId)
            .orElseThrow(() -> new ResourceNotFoundException("Tracked product not found"));
        
        // Update fields if present in request
        if (request.desiredPrice() != null) {
            trackedProduct.setDesiredPrice(request.desiredPrice());
        }
        if (request.notificationEnabled() != null) {
            trackedProduct.setNotificationEnabled(request.notificationEnabled());
        }
        
        return trackedProductRepository.save(trackedProduct);
    }
    
    @Transactional
    public void deleteTrackedProduct(Long userId, Long trackedProductId) {
        TrackedProduct trackedProduct = getTrackedProductById(userId, trackedProductId)
            .orElseThrow(() -> new ResourceNotFoundException("Tracked product not found"));
        
        trackedProductRepository.delete(trackedProduct);
    }
} 