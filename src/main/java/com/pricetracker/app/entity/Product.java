package com.pricetracker.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing a product being tracked.
 */
@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_url", columnList = "product_url")
})
@Getter
@Setter
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_url", nullable = false, unique = true, length = 1024)
    private String productUrl;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "last_checked_price", precision = 10, scale = 2)
    private BigDecimal lastCheckedPrice;

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

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProductUrl() {
        return productUrl;
    }

    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public BigDecimal getLastCheckedPrice() {
        return lastCheckedPrice;
    }

    public void setLastCheckedPrice(BigDecimal lastCheckedPrice) {
        this.lastCheckedPrice = lastCheckedPrice;
    }
} 