package com.pricetracker.app.repository;

import com.pricetracker.app.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void whenSaveProduct_thenProductIsSaved() {
        // Given
        Product product = new Product();
        product.setProductUrl("https://example.com/product");
        product.setName("Test Product");
        product.setImageUrl("https://example.com/image.jpg");
        product.setLastCheckedPrice(new BigDecimal("99.99"));

        // When
        Product savedProduct = productRepository.save(product);

        // Then
        assertThat(savedProduct.getId()).isNotNull();
        assertThat(savedProduct.getProductUrl()).isEqualTo("https://example.com/product");
        assertThat(savedProduct.getName()).isEqualTo("Test Product");
    }

    @Test
    void whenFindById_thenProductIsFound() {
        // Given
        Product product = new Product();
        product.setProductUrl("https://example.com/product");
        product.setName("Test Product");
        Product savedProduct = entityManager.persist(product);
        entityManager.flush();

        // When
        Optional<Product> foundProduct = productRepository.findById(savedProduct.getId());

        // Then
        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getName()).isEqualTo("Test Product");
    }

    @Test
    void whenFindByProductUrl_thenProductIsFound() {
        // Given
        Product product = new Product();
        product.setProductUrl("https://example.com/product");
        product.setName("Test Product");
        entityManager.persist(product);
        entityManager.flush();

        // When
        Optional<Product> foundProduct = productRepository.findByProductUrl("https://example.com/product");

        // Then
        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getName()).isEqualTo("Test Product");
    }
} 