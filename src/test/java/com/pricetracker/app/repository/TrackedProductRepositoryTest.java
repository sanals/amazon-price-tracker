package com.pricetracker.app.repository;

import com.pricetracker.app.entity.Product;
import com.pricetracker.app.entity.TrackedProduct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TrackedProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TrackedProductRepository trackedProductRepository;

    private static final Long TEST_USER_ID = 1L;

    @Test
    void whenSaveTrackedProduct_thenTrackedProductIsSaved() {
        // Given
        Product product = createTestProduct();
        TrackedProduct trackedProduct = new TrackedProduct();
        trackedProduct.setUserId(TEST_USER_ID);
        trackedProduct.setProduct(product);
        trackedProduct.setDesiredPrice(new BigDecimal("89.99"));
        trackedProduct.setNotificationEnabled(true);

        // When
        TrackedProduct savedTrackedProduct = trackedProductRepository.save(trackedProduct);

        // Then
        assertThat(savedTrackedProduct.getId()).isNotNull();
        assertThat(savedTrackedProduct.getDesiredPrice()).isEqualTo(new BigDecimal("89.99"));
        assertThat(savedTrackedProduct.isNotificationEnabled()).isTrue();
    }

    @Test
    void whenFindById_thenTrackedProductIsFound() {
        // Given
        Product product = createTestProduct();
        TrackedProduct trackedProduct = new TrackedProduct();
        trackedProduct.setUserId(TEST_USER_ID);
        trackedProduct.setProduct(product);
        trackedProduct.setDesiredPrice(new BigDecimal("89.99"));
        TrackedProduct savedTrackedProduct = entityManager.persist(trackedProduct);
        entityManager.flush();

        // When
        Optional<TrackedProduct> foundTrackedProduct = trackedProductRepository.findById(savedTrackedProduct.getId());

        // Then
        assertThat(foundTrackedProduct).isPresent();
        assertThat(foundTrackedProduct.get().getDesiredPrice()).isEqualTo(new BigDecimal("89.99"));
    }

    @Test
    void whenFindByUserIdAndProductId_thenTrackedProductIsFound() {
        // Given
        Product product = createTestProduct();
        TrackedProduct trackedProduct = new TrackedProduct();
        trackedProduct.setUserId(TEST_USER_ID);
        trackedProduct.setProduct(product);
        trackedProduct.setDesiredPrice(new BigDecimal("89.99"));
        entityManager.persist(trackedProduct);
        entityManager.flush();

        // When
        Optional<TrackedProduct> foundTrackedProduct = trackedProductRepository.findByUserIdAndProductId(TEST_USER_ID, product.getId());

        // Then
        assertThat(foundTrackedProduct).isPresent();
        assertThat(foundTrackedProduct.get().getDesiredPrice()).isEqualTo(new BigDecimal("89.99"));
    }

    @Test
    void whenExistsByUserIdAndProductId_thenReturnsTrue() {
        // Given
        Product product = createTestProduct();
        TrackedProduct trackedProduct = new TrackedProduct();
        trackedProduct.setUserId(TEST_USER_ID);
        trackedProduct.setProduct(product);
        trackedProduct.setDesiredPrice(new BigDecimal("89.99"));
        entityManager.persist(trackedProduct);
        entityManager.flush();

        // When
        boolean exists = trackedProductRepository.existsByUserIdAndProductId(TEST_USER_ID, product.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void whenFindByUserId_thenReturnsPageOfTrackedProducts() {
        // Given
        Product product1 = createTestProduct();
        Product product2 = createTestProduct();
        
        TrackedProduct trackedProduct1 = new TrackedProduct();
        trackedProduct1.setUserId(TEST_USER_ID);
        trackedProduct1.setProduct(product1);
        trackedProduct1.setDesiredPrice(new BigDecimal("89.99"));
        
        TrackedProduct trackedProduct2 = new TrackedProduct();
        trackedProduct2.setUserId(TEST_USER_ID);
        trackedProduct2.setProduct(product2);
        trackedProduct2.setDesiredPrice(new BigDecimal("99.99"));
        
        entityManager.persist(trackedProduct1);
        entityManager.persist(trackedProduct2);
        entityManager.flush();

        // When
        Page<TrackedProduct> page = trackedProductRepository.findByUserId(TEST_USER_ID, PageRequest.of(0, 10));

        // Then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting("desiredPrice")
            .containsExactlyInAnyOrder(new BigDecimal("89.99"), new BigDecimal("99.99"));
    }

    private Product createTestProduct() {
        Product product = new Product();
        product.setProductUrl("https://example.com/product/" + java.util.UUID.randomUUID().toString());
        product.setName("Test Product");
        return entityManager.persist(product);
    }
} 