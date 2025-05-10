package com.pricetracker.app.controller;

import com.pricetracker.app.dto.request.TrackProductRequest;
import com.pricetracker.app.dto.request.UpdateTrackedProductRequest;
import com.pricetracker.app.dto.response.ApiResponse;
import com.pricetracker.app.dto.response.PriceHistoryResponse;
import com.pricetracker.app.dto.response.ProductResponse;
import com.pricetracker.app.dto.response.TrackedProductResponse;
import com.pricetracker.app.entity.PriceHistory;
import com.pricetracker.app.entity.Product;
import com.pricetracker.app.entity.TrackedProduct;
import com.pricetracker.app.exception.ResourceNotFoundException;
import com.pricetracker.app.repository.PriceHistoryRepository;
import com.pricetracker.app.repository.ProductRepository;
import com.pricetracker.app.service.ProductTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST controller for product tracking operations.
 * Temporary implementation without security - uses userId path variables instead.
 */
@RestController
@RequestMapping("/track")
@RequiredArgsConstructor
public class ProductTrackingController {
    
    private final ProductTrackingService productTrackingService;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductRepository productRepository;
    
    /**
     * Add a new product URL to track for a user.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TrackedProductResponse>> addProductTracking(
            @RequestParam Long userId,
            @RequestBody TrackProductRequest request) {
        
        TrackedProduct trackedProduct = productTrackingService.addProductTracking(userId, request);
        TrackedProductResponse response = mapToTrackedProductResponse(trackedProduct);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Product tracking added successfully"));
    }
    
    /**
     * Get all tracked products for a user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<TrackedProductResponse>>> getTrackedProducts(
            @PathVariable Long userId,
            Pageable pageable) {
        
        Page<TrackedProduct> trackedProducts = productTrackingService.getTrackedProductsForUser(userId, pageable);
        Page<TrackedProductResponse> response = trackedProducts.map(this::mapToTrackedProductResponse);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Tracked products retrieved successfully"));
    }
    
    /**
     * Get a specific tracked product by ID.
     */
    @GetMapping("/{trackedProductId}/user/{userId}")
    public ResponseEntity<ApiResponse<TrackedProductResponse>> getTrackedProduct(
            @PathVariable Long trackedProductId,
            @PathVariable Long userId) {
        
        TrackedProduct trackedProduct = productTrackingService.getTrackedProductById(userId, trackedProductId)
                .orElseThrow(() -> new ResourceNotFoundException("TrackedProduct", "id", trackedProductId));
        
        TrackedProductResponse response = mapToTrackedProductResponse(trackedProduct);
        return ResponseEntity.ok(ApiResponse.success(response, "Tracked product retrieved successfully"));
    }
    
    /**
     * Update a tracked product's desired price or notification status.
     */
    @PutMapping("/{trackedProductId}/user/{userId}")
    public ResponseEntity<ApiResponse<TrackedProductResponse>> updateTrackedProduct(
            @PathVariable Long trackedProductId,
            @PathVariable Long userId,
            @RequestBody UpdateTrackedProductRequest request) {
        
        TrackedProduct updatedTrackedProduct = 
                productTrackingService.updateTrackedProduct(userId, trackedProductId, request);
        
        TrackedProductResponse response = mapToTrackedProductResponse(updatedTrackedProduct);
        return ResponseEntity.ok(ApiResponse.success(response, "Tracked product updated successfully"));
    }
    
    /**
     * Delete (stop tracking) a product.
     */
    @DeleteMapping("/{trackedProductId}/user/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteTrackedProduct(
            @PathVariable Long trackedProductId,
            @PathVariable Long userId) {
        
        productTrackingService.deleteTrackedProduct(userId, trackedProductId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(null, "Tracked product deleted successfully"));
    }
    
    /**
     * Get price history for a specific product.
     * Note: This endpoint currently has no user association check.
     */
    @GetMapping("/product/{productId}/history")
    public ResponseEntity<ApiResponse<Page<PriceHistoryResponse>>> getPriceHistory(
            @PathVariable Long productId,
            Pageable pageable) {
        
        Page<PriceHistory> priceHistoryPage = 
                priceHistoryRepository.findByProductIdOrderByTimestampDesc(productId, pageable);
        
        if (priceHistoryPage.isEmpty()) {
            // Check if product exists
            productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        }
        
        Page<PriceHistoryResponse> response = priceHistoryPage.map(this::mapToPriceHistoryResponse);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Price history retrieved successfully"));
    }
    
    /**
     * Map a TrackedProduct entity to a TrackedProductResponse DTO.
     */
    private TrackedProductResponse mapToTrackedProductResponse(TrackedProduct trackedProduct) {
        Product product = trackedProduct.getProduct();
        
        ProductResponse productResponse = new ProductResponse(
            product.getId(),
            product.getProductUrl(),
            product.getName(),
            product.getImageUrl(),
            product.getLastCheckedPrice(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
        
        return new TrackedProductResponse(
            trackedProduct.getId(),
            productResponse,
            trackedProduct.getDesiredPrice(),
            trackedProduct.isNotificationEnabled(),
            trackedProduct.getCreatedAt(),
            trackedProduct.getUpdatedAt()
        );
    }
    
    /**
     * Map a PriceHistory entity to a PriceHistoryResponse DTO.
     */
    private PriceHistoryResponse mapToPriceHistoryResponse(PriceHistory priceHistory) {
        return new PriceHistoryResponse(
                priceHistory.getId(),
                priceHistory.getPrice(),
                priceHistory.getTimestamp() != null ? priceHistory.getTimestamp() : Instant.now()
        );
    }
} 