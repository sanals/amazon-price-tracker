package com.pricetracker.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricetracker.app.dto.request.TrackProductRequest;
import com.pricetracker.app.dto.request.UpdateTrackedProductRequest;
import com.pricetracker.app.entity.PriceHistory;
import com.pricetracker.app.entity.Product;
import com.pricetracker.app.entity.TrackedProduct;
import com.pricetracker.app.exception.ResourceNotFoundException;
import com.pricetracker.app.exception.ProductAlreadyTrackedException;
import com.pricetracker.app.repository.PriceHistoryRepository;
import com.pricetracker.app.repository.ProductRepository;
import com.pricetracker.app.service.ProductTrackingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.test.mockmvc.print=default",
    "spring.main.allow-bean-definition-overriding=true"
})
public class ProductTrackingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SuppressWarnings("removal")
	@MockBean
    private ProductTrackingService productTrackingService;

    @SuppressWarnings("removal")
	@MockBean
    private PriceHistoryRepository priceHistoryRepository;
    
    @SuppressWarnings("removal")
	@MockBean
    private ProductRepository productRepository;

    @Test
    @DisplayName("Add product tracking - success")
    public void testAddProductTracking() throws Exception {
        // Arrange
        Long userId = 1L;
        TrackProductRequest request = new TrackProductRequest(
                "https://www.amazon.com/dp/B08F5F1TN4", 
                new BigDecimal("449.99")
        );
        
        Product product = createTestProduct();
        TrackedProduct trackedProduct = createTestTrackedProduct(product);
        
        when(productTrackingService.addProductTracking(eq(userId), any(TrackProductRequest.class)))
                .thenReturn(trackedProduct);

        // Act & Assert
        mockMvc.perform(post("/track")
                .param("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Product tracking added successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.product.productId").value(1))
                .andExpect(jsonPath("$.data.product.name").value("PlayStation 5 Console"))
                .andExpect(jsonPath("$.data.desiredPrice").value(449.99));
        
        verify(productTrackingService).addProductTracking(eq(userId), any(TrackProductRequest.class));
    }
    
    @Test
    @DisplayName("Add product tracking - already tracked")
    public void testAddProductTracking_AlreadyTracked() throws Exception {
        // Arrange
        Long userId = 1L;
        TrackProductRequest request = new TrackProductRequest(
                "https://www.amazon.com/dp/B08F5F1TN4", 
                new BigDecimal("449.99")
        );
        
        when(productTrackingService.addProductTracking(eq(userId), any(TrackProductRequest.class)))
                .thenThrow(new ProductAlreadyTrackedException("You are already tracking this product"));

        // Act & Assert
        mockMvc.perform(post("/track")
                .param("userId", userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("You are already tracking this product"));
        
        verify(productTrackingService).addProductTracking(eq(userId), any(TrackProductRequest.class));
    }
    
    @Test
    @DisplayName("Get all tracked products for user - success")
    public void testGetTrackedProducts() throws Exception {
        // Arrange
        Long userId = 1L;
        Product product = createTestProduct();
        TrackedProduct trackedProduct = createTestTrackedProduct(product);
        
        // Use a specific PageRequest instead of relying on an Unpaged instance
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TrackedProduct> trackedProductPage = new PageImpl<>(List.of(trackedProduct), pageable, 1);
        
        when(productTrackingService.getTrackedProductsForUser(eq(userId), any(Pageable.class)))
                .thenReturn(trackedProductPage);

        // Act & Assert
        mockMvc.perform(get("/track/user/{userId}", userId)
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Tracked products retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].product.name").value("PlayStation 5 Console"));
        
        verify(productTrackingService).getTrackedProductsForUser(eq(userId), any(Pageable.class));
    }
    
    @Test
    @DisplayName("Get specific tracked product - success")
    public void testGetTrackedProduct() throws Exception {
        // Arrange
        Long userId = 1L;
        Long trackedProductId = 1L;
        Product product = createTestProduct();
        TrackedProduct trackedProduct = createTestTrackedProduct(product);
        
        when(productTrackingService.getTrackedProductById(userId, trackedProductId))
                .thenReturn(Optional.of(trackedProduct));

        // Act & Assert
        mockMvc.perform(get("/track/{trackedProductId}/user/{userId}", trackedProductId, userId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Tracked product retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.product.productId").value(1))
                .andExpect(jsonPath("$.data.product.name").value("PlayStation 5 Console"));
        
        verify(productTrackingService).getTrackedProductById(userId, trackedProductId);
    }
    
    @Test
    @DisplayName("Get specific tracked product - not found")
    public void testGetTrackedProduct_NotFound() throws Exception {
        // Arrange
        Long userId = 1L;
        Long trackedProductId = 999L;
        
        when(productTrackingService.getTrackedProductById(userId, trackedProductId))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/track/{trackedProductId}/user/{userId}", trackedProductId, userId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value(404));
        
        verify(productTrackingService).getTrackedProductById(userId, trackedProductId);
    }
    
    @Test
    @DisplayName("Update tracked product - success")
    public void testUpdateTrackedProduct() throws Exception {
        // Arrange
        Long userId = 1L;
        Long trackedProductId = 1L;
        UpdateTrackedProductRequest request = new UpdateTrackedProductRequest(
                new BigDecimal("429.99"),
                true
        );
        
        Product product = createTestProduct();
        TrackedProduct trackedProduct = createTestTrackedProduct(product);
        trackedProduct.setDesiredPrice(new BigDecimal("429.99")); // Updated price
        
        when(productTrackingService.updateTrackedProduct(eq(userId), eq(trackedProductId), any(UpdateTrackedProductRequest.class)))
                .thenReturn(trackedProduct);

        // Act & Assert
        mockMvc.perform(put("/track/{trackedProductId}/user/{userId}", trackedProductId, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Tracked product updated successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.desiredPrice").value(429.99));
        
        verify(productTrackingService).updateTrackedProduct(eq(userId), eq(trackedProductId), any(UpdateTrackedProductRequest.class));
    }
    
    @Test
    @DisplayName("Update tracked product - not found")
    public void testUpdateTrackedProduct_NotFound() throws Exception {
        // Arrange
        Long userId = 1L;
        Long trackedProductId = 999L;
        UpdateTrackedProductRequest request = new UpdateTrackedProductRequest(
                new BigDecimal("429.99"),
                true
        );
        
        when(productTrackingService.updateTrackedProduct(eq(userId), eq(trackedProductId), any(UpdateTrackedProductRequest.class)))
                .thenThrow(new ResourceNotFoundException("Tracked product not found"));

        // Act & Assert
        mockMvc.perform(put("/track/{trackedProductId}/user/{userId}", trackedProductId, userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value(404));
        
        verify(productTrackingService).updateTrackedProduct(eq(userId), eq(trackedProductId), any(UpdateTrackedProductRequest.class));
    }
    
    @Test
    @DisplayName("Delete tracked product - success")
    public void testDeleteTrackedProduct() throws Exception {
        // Arrange
        Long userId = 1L;
        Long trackedProductId = 1L;
        
        doNothing().when(productTrackingService).deleteTrackedProduct(userId, trackedProductId);

        // Act & Assert
        mockMvc.perform(delete("/track/{trackedProductId}/user/{userId}", trackedProductId, userId))
                .andExpect(status().isNoContent())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Tracked product deleted successfully"));
        
        verify(productTrackingService).deleteTrackedProduct(userId, trackedProductId);
    }
    
    @Test
    @DisplayName("Delete tracked product - not found")
    public void testDeleteTrackedProduct_NotFound() throws Exception {
        // Arrange
        Long userId = 1L;
        Long trackedProductId = 999L;
        
        doThrow(new ResourceNotFoundException("Tracked product not found"))
                .when(productTrackingService).deleteTrackedProduct(userId, trackedProductId);

        // Act & Assert
        mockMvc.perform(delete("/track/{trackedProductId}/user/{userId}", trackedProductId, userId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value(404));
        
        verify(productTrackingService).deleteTrackedProduct(userId, trackedProductId);
    }
    
    @Test
    @DisplayName("Get price history - success")
    public void testGetPriceHistory() throws Exception {
        // Arrange
        Long productId = 1L;
        PriceHistory priceHistory1 = createPriceHistory(productId, new BigDecimal("499.99"), Instant.now().minusSeconds(86400)); // 1 day ago
        PriceHistory priceHistory2 = createPriceHistory(productId, new BigDecimal("489.99"), Instant.now()); // Now
        
        // Use a specific PageRequest instead of relying on an Unpaged instance
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<PriceHistory> priceHistoryPage = new PageImpl<>(List.of(priceHistory2, priceHistory1), pageable, 2);
        
        when(priceHistoryRepository.findByProductIdOrderByTimestampDesc(eq(productId), any(Pageable.class)))
                .thenReturn(priceHistoryPage);

        // Act & Assert
        mockMvc.perform(get("/track/product/{productId}/history", productId)
                .param("page", "0")
                .param("size", "10")
                .param("sort", "timestamp,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Price history retrieved successfully"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].price").value(489.99));
        
        verify(priceHistoryRepository).findByProductIdOrderByTimestampDesc(eq(productId), any(Pageable.class));
    }
    
    @Test
    @DisplayName("Get price history - product not found")
    public void testGetPriceHistory_ProductNotFound() throws Exception {
        // Arrange
        Long productId = 999L;
        
        // Return empty page
        when(priceHistoryRepository.findByProductIdOrderByTimestampDesc(eq(productId), any(Pageable.class)))
                .thenReturn(Page.empty());
        
        // Product repository find returns empty
        when(productRepository.findById(productId))
                .thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/track/product/{productId}/history", productId)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.code").value(404));
        
        verify(priceHistoryRepository).findByProductIdOrderByTimestampDesc(eq(productId), any(Pageable.class));
        verify(productRepository).findById(productId);
    }
    
    // Helper methods to create test entities
    
    private Product createTestProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setProductUrl("https://www.amazon.com/dp/B08F5F1TN4");
        product.setName("PlayStation 5 Console");
        product.setImageUrl("https://m.media-amazon.com/images/I/51051FiD9UL._AC_SX679_.jpg");
        product.setLastCheckedPrice(new BigDecimal("499.99"));
        product.setCreatedAt(Instant.now().minusSeconds(3600)); // 1 hour ago
        product.setUpdatedAt(Instant.now());
        return product;
    }
    
    private TrackedProduct createTestTrackedProduct(Product product) {
        TrackedProduct trackedProduct = new TrackedProduct();
        trackedProduct.setId(1L);
        trackedProduct.setUserId(1L);
        trackedProduct.setProduct(product);
        trackedProduct.setDesiredPrice(new BigDecimal("449.99"));
        trackedProduct.setNotificationEnabled(true);
        trackedProduct.setCreatedAt(Instant.now().minusSeconds(1800)); // 30 minutes ago
        trackedProduct.setUpdatedAt(Instant.now());
        return trackedProduct;
    }
    
    private PriceHistory createPriceHistory(Long productId, BigDecimal price, Instant timestamp) {
        PriceHistory priceHistory = new PriceHistory();
        priceHistory.setId(productId * 10 + (int)(Math.random() * 10)); // Generate unique ID
        Product product = new Product();
        product.setId(productId);
        priceHistory.setProduct(product);
        priceHistory.setPrice(price);
        priceHistory.setTimestamp(timestamp);
        return priceHistory;
    }
} 