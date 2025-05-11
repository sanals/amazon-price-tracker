package com.pricetracker.app.scraping;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsoupScraperServiceTest {

    @Mock
    private AmazonScraperStrategy amazonScraperStrategy;

    // Don't use @InjectMocks here since we need to manually inject the constructor
    private JsoupScraperService scraperService;

    private static final String TEST_URL = "https://example.com/product";

    @BeforeEach
    void setUp() {
        // Mock the expandShortenedUrl method to return the same URL to avoid NPE
        when(amazonScraperStrategy.expandShortenedUrl(anyString())).thenReturn(TEST_URL);
        when(amazonScraperStrategy.canHandle(anyString())).thenReturn(true);
        
        // Manually create the service using the constructor
        scraperService = new JsoupScraperService(amazonScraperStrategy);
        
        // Set a positive value for defaultDelayMs to avoid IllegalArgumentException
        ReflectionTestUtils.setField(scraperService, "defaultDelayMs", 100);
    }

    @Test
    void whenScrapePrice_withValidDocument_thenReturnPrice() throws IOException {
        // Given
        Document mockDocument = mock(Document.class);
        Connection mockConnection = mock(Connection.class);

        try (MockedStatic<Jsoup> jsoup = Mockito.mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
            when(mockConnection.userAgent(anyString())).thenReturn(mockConnection);
            when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);
            when(mockConnection.headers(anyMap())).thenReturn(mockConnection);
            when(mockConnection.followRedirects(anyBoolean())).thenReturn(mockConnection);
            when(mockConnection.maxBodySize(anyInt())).thenReturn(mockConnection);
            when(mockConnection.ignoreContentType(anyBoolean())).thenReturn(mockConnection);
            when(mockConnection.ignoreHttpErrors(anyBoolean())).thenReturn(mockConnection);
            when(mockConnection.get()).thenReturn(mockDocument);
            
            when(amazonScraperStrategy.extractPrice(mockDocument)).thenReturn(Optional.of(new BigDecimal("99.99")));

            // When
            Optional<BigDecimal> result = scraperService.scrapePrice(TEST_URL);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(new BigDecimal("99.99"));
        }
    }

    @Test
    void whenScrapeProductDetails_withValidDocument_thenReturnDetails() throws IOException {
        // Given
        Document mockDocument = mock(Document.class);
        Connection mockConnection = mock(Connection.class);
        ProductDetails mockDetails = new ProductDetails(
            Optional.of("Test Product"),
            Optional.of("https://example.com/image.jpg"),
            Optional.of(new BigDecimal("99.99"))
        );

        try (MockedStatic<Jsoup> jsoup = Mockito.mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
            when(mockConnection.userAgent(anyString())).thenReturn(mockConnection);
            when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);
            when(mockConnection.headers(anyMap())).thenReturn(mockConnection);
            when(mockConnection.followRedirects(anyBoolean())).thenReturn(mockConnection);
            when(mockConnection.maxBodySize(anyInt())).thenReturn(mockConnection);
            when(mockConnection.ignoreContentType(anyBoolean())).thenReturn(mockConnection);
            when(mockConnection.ignoreHttpErrors(anyBoolean())).thenReturn(mockConnection);
            when(mockConnection.get()).thenReturn(mockDocument);
            
            when(amazonScraperStrategy.scrapeProductDetails(mockDocument)).thenReturn(Optional.of(mockDetails));

            // When
            Optional<ProductDetails> result = scraperService.scrapeProductDetails(TEST_URL);

            // Then
            assertThat(result).isPresent();
            ProductDetails details = result.get();
            assertThat(details.name()).contains("Test Product");
            assertThat(details.imageUrl()).contains("https://example.com/image.jpg");
            assertThat(details.price()).contains(new BigDecimal("99.99"));
        }
    }

    @Test
    void whenScrapePrice_withIOException_thenReturnEmpty() throws IOException {
        // Given
        Connection mockConnection = mock(Connection.class);

        try (MockedStatic<Jsoup> jsoup = Mockito.mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
            
            // Use lenient() to avoid UnnecessaryStubbingException
            lenient().when(mockConnection.userAgent(anyString())).thenReturn(mockConnection);
            lenient().when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);
            lenient().when(mockConnection.headers(anyMap())).thenReturn(mockConnection);
            lenient().when(mockConnection.followRedirects(anyBoolean())).thenReturn(mockConnection);
            lenient().when(mockConnection.maxBodySize(anyInt())).thenReturn(mockConnection);
            lenient().when(mockConnection.ignoreContentType(anyBoolean())).thenReturn(mockConnection);
            lenient().when(mockConnection.ignoreHttpErrors(anyBoolean())).thenReturn(mockConnection);
            
            // This is the only stub that will actually be used
            when(mockConnection.get()).thenThrow(new IOException("Connection failed"));

            // When
            Optional<BigDecimal> result = scraperService.scrapePrice(TEST_URL);

            // Then
            assertThat(result).isEmpty();
        }
    }
} 