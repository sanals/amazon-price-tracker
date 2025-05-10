package com.pricetracker.app.repository;

import com.pricetracker.app.entity.PriceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for managing PriceHistory entities.
 */
@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    
    /**
     * Find price history records for a product, ordered by timestamp.
     * 
     * @param productId the ID of the product
     * @param pageable pagination information
     * @return a page of price history records
     */
    Page<PriceHistory> findByProductIdOrderByTimestampDesc(Long productId, Pageable pageable);
    
    /**
     * Find price history records for a product within a date range.
     * 
     * @param productId the ID of the product
     * @param startDate the start date of the range
     * @param endDate the end date of the range
     * @return a list of price history records
     */
    List<PriceHistory> findByProductIdAndTimestampBetweenOrderByTimestampDesc(
        Long productId, Instant startDate, Instant endDate);
} 