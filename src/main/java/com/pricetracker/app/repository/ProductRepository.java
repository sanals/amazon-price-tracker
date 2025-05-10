package com.pricetracker.app.repository;

import com.pricetracker.app.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing Product entities.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    /**
     * Find a product by its URL.
     * 
     * @param productUrl the URL of the product
     * @return an Optional containing the product if found
     */
    Optional<Product> findByProductUrl(String productUrl);
} 