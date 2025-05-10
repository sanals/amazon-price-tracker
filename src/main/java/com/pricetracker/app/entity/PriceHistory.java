package com.pricetracker.app.entity;

import com.pricetracker.app.entity.base.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entity representing the price history of a product.
 */
@Entity
@Table(name = "price_history", indexes = {
    @Index(name = "idx_price_history_product", columnList = "product_id"),
    @Index(name = "idx_price_history_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
public class PriceHistory extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Instant timestamp;
} 