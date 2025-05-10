package com.pricetracker.app.repository;

import com.pricetracker.app.entity.PriceHistory;
import com.pricetracker.app.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PriceHistoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    @Test
    void whenSavePriceHistory_thenPriceHistoryIsSaved() {
        // Given
        Product product = createTestProduct();
        PriceHistory priceHistory = new PriceHistory();
        priceHistory.setProduct(product);
        priceHistory.setPrice(new BigDecimal("99.99"));
        priceHistory.setTimestamp(Instant.now());

        // When
        PriceHistory savedPriceHistory = priceHistoryRepository.save(priceHistory);

        // Then
        assertThat(savedPriceHistory.getId()).isNotNull();
        assertThat(savedPriceHistory.getPrice()).isEqualTo(new BigDecimal("99.99"));
    }

    @Test
    void whenFindById_thenPriceHistoryIsFound() {
        // Given
        Product product = createTestProduct();
        PriceHistory priceHistory = new PriceHistory();
        priceHistory.setProduct(product);
        priceHistory.setPrice(new BigDecimal("99.99"));
        priceHistory.setTimestamp(Instant.now());
        PriceHistory savedPriceHistory = entityManager.persist(priceHistory);
        entityManager.flush();

        // When
        Optional<PriceHistory> foundPriceHistory = priceHistoryRepository.findById(savedPriceHistory.getId());

        // Then
        assertThat(foundPriceHistory).isPresent();
        assertThat(foundPriceHistory.get().getPrice()).isEqualTo(new BigDecimal("99.99"));
    }

    @Test
    void whenFindByProductIdOrderByTimestampDesc_thenReturnsPageOfPriceHistory() {
        // Given
        Product product = createTestProduct();
        Instant now = Instant.now();
        
        PriceHistory priceHistory1 = new PriceHistory();
        priceHistory1.setProduct(product);
        priceHistory1.setPrice(new BigDecimal("89.99"));
        priceHistory1.setTimestamp(now.minusSeconds(3600)); // 1 hour ago
        
        PriceHistory priceHistory2 = new PriceHistory();
        priceHistory2.setProduct(product);
        priceHistory2.setPrice(new BigDecimal("99.99"));
        priceHistory2.setTimestamp(now);
        
        entityManager.persist(priceHistory1);
        entityManager.persist(priceHistory2);
        entityManager.flush();

        // When
        Page<PriceHistory> page = priceHistoryRepository.findByProductIdOrderByTimestampDesc(product.getId(), PageRequest.of(0, 10));

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting("price")
            .containsExactly(new BigDecimal("99.99"), new BigDecimal("89.99")); // Should be in descending order
    }

    private Product createTestProduct() {
        Product product = new Product();
        product.setProductUrl("https://example.com/product");
        product.setName("Test Product");
        return entityManager.persist(product);
    }
} 