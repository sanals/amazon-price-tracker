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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Product Tracking", description = "API for tracking product prices")
public class ProductTrackingController {
    
    private final ProductTrackingService productTrackingService;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductRepository productRepository;
    
    /**
     * Add a new product URL to track for a user.
     */
    @PostMapping
    @Operation(summary = "Track a new product", description = "Add a new product URL to track for a specific user")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product successfully tracked"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<ApiResponse<TrackedProductResponse>> addProductTracking(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Product tracking details") @RequestBody TrackProductRequest request) {
        
        TrackedProduct trackedProduct = productTrackingService.addProductTracking(userId, request);
        TrackedProductResponse response = mapToTrackedProductResponse(trackedProduct);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Product tracking added successfully"));
    }
    
    /**
     * Get all tracked products for a user.
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get tracked products", description = "Retrieve all products tracked by a specific user")
    public ResponseEntity<ApiResponse<Page<TrackedProductResponse>>> getTrackedProducts(
            @Parameter(description = "User ID") @PathVariable Long userId,
            Pageable pageable) {
        
        Page<TrackedProduct> trackedProducts = productTrackingService.getTrackedProductsForUser(userId, pageable);
        Page<TrackedProductResponse> response = trackedProducts.map(this::mapToTrackedProductResponse);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Tracked products retrieved successfully"));
    }
    
    /**
     * Get a specific tracked product by ID.
     */
    @GetMapping("/{trackedProductId}/user/{userId}")
    @Operation(summary = "Get specific tracked product", description = "Get details of a specific product tracking entry")
    public ResponseEntity<ApiResponse<TrackedProductResponse>> getTrackedProduct(
            @Parameter(description = "Tracked product ID") @PathVariable Long trackedProductId,
            @Parameter(description = "User ID") @PathVariable Long userId) {
        
        TrackedProduct trackedProduct = productTrackingService.getTrackedProductById(userId, trackedProductId)
                .orElseThrow(() -> new ResourceNotFoundException("TrackedProduct", "id", trackedProductId));
        
        TrackedProductResponse response = mapToTrackedProductResponse(trackedProduct);
        return ResponseEntity.ok(ApiResponse.success(response, "Tracked product retrieved successfully"));
    }
    
    /**
     * Update a tracked product's desired price or notification status.
     */
    @PutMapping("/{trackedProductId}/user/{userId}")
    @Operation(summary = "Update tracked product", description = "Update the desired price or notification settings for a tracked product")
    public ResponseEntity<ApiResponse<TrackedProductResponse>> updateTrackedProduct(
            @Parameter(description = "Tracked product ID") @PathVariable Long trackedProductId,
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Updated tracking details") @RequestBody UpdateTrackedProductRequest request) {
        
        TrackedProduct updatedTrackedProduct = 
                productTrackingService.updateTrackedProduct(userId, trackedProductId, request);
        
        TrackedProductResponse response = mapToTrackedProductResponse(updatedTrackedProduct);
        return ResponseEntity.ok(ApiResponse.success(response, "Tracked product updated successfully"));
    }
    
    /**
     * Delete (stop tracking) a product.
     */
    @DeleteMapping("/{trackedProductId}/user/{userId}")
    @Operation(summary = "Delete tracked product", description = "Stop tracking a product")
    public ResponseEntity<ApiResponse<Void>> deleteTrackedProduct(
            @Parameter(description = "Tracked product ID") @PathVariable Long trackedProductId,
            @Parameter(description = "User ID") @PathVariable Long userId) {
        
        productTrackingService.deleteTrackedProduct(userId, trackedProductId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(null, "Tracked product deleted successfully"));
    }
    
    /**
     * Get price history for a specific product.
     * Note: This endpoint currently has no user association check.
     */
    @GetMapping("/product/{productId}/history")
    @Operation(summary = "Get price history", description = "Retrieve price history for a specific product")
    public ResponseEntity<ApiResponse<Page<PriceHistoryResponse>>> getPriceHistory(
            @Parameter(description = "Product ID") @PathVariable Long productId,
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