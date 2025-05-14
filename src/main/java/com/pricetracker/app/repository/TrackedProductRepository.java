package com.pricetracker.app.repository;

import com.pricetracker.app.entity.TrackedProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing TrackedProduct entities.
 */
@Repository
public interface TrackedProductRepository extends JpaRepository<TrackedProduct, Long> {
    
    /**
     * Find a tracked product by user ID and product ID.
     * 
     * @param userId the ID of the user
     * @param productId the ID of the product
     * @return an Optional containing the tracked product if found
     */
    Optional<TrackedProduct> findByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * Check if a tracked product exists for the given user and product.
     * 
     * @param userId the ID of the user
     * @param productId the ID of the product
     * @return true if a tracked product exists
     */
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * Find all tracked products for a user with pagination.
     * 
     * @param userId the ID of the user
     * @param pageable pagination information
     * @return a page of tracked products
     */
    Page<TrackedProduct> findByUserId(Long userId, Pageable pageable);
    
    /**
     * Find all tracked products for a product that have notifications enabled.
     * 
     * @param productId the ID of the product
     * @return a list of tracked products with notifications enabled
     */
    List<TrackedProduct> findByProductIdAndNotificationEnabledTrue(Long productId);

    Optional<TrackedProduct> findByIdAndUserId(Long id, Long userId);
    
    /**
     * Find all tracked products for a specific product.
     * 
     * @param productId the ID of the product
     * @return a list of tracked products
     */
    List<TrackedProduct> findByProductId(Long productId);
} 