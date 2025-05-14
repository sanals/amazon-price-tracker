package com.pricetracker.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing a product being tracked by a user.
 */
@Entity
@Table(name = "tracked_products", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "product_id"})
})
@Getter
@Setter
public class TrackedProduct {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal desiredPrice;
    
    @Column(nullable = false)
    private boolean notificationEnabled = true;
    
    @Column
    private Instant lastNotifiedAt;
    
    /**
     * The interval in minutes at which to check this product's price.
     * Minimum value enforced by the system is 5 minutes.
     */
    @Column(nullable = false)
    private Integer checkIntervalMinutes = 60; // Default: 1 hour in minutes
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
} 