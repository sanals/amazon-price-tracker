package com.pricetracker.app.scraping;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsoupScraperServiceTest {

    @InjectMocks
    private JsoupScraperService scraperService;

    private static final String TEST_URL = "https://example.com/product";
    private static final String USER_AGENT = "TestUserAgent";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scraperService, "userAgent", USER_AGENT);
    }

    @Test
    void whenScrapePrice_withValidDocument_thenReturnPrice() throws IOException {
        // Given
        Document mockDocument = mock(Document.class);
        Element mockElement = mock(Element.class);
        Connection mockConnection = mock(Connection.class);

        try (MockedStatic<Jsoup> jsoup = Mockito.mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
            when(mockConnection.userAgent(anyString())).thenReturn(mockConnection);
            when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);
            when(mockConnection.get()).thenReturn(mockDocument);
            when(mockDocument.selectFirst(anyString())).thenReturn(mockElement);
            when(mockElement.text()).thenReturn("$99.99");

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
        Element mockPriceElement = mock(Element.class);
        Element mockNameElement = mock(Element.class);
        Element mockImageElement = mock(Element.class);
        Connection mockConnection = mock(Connection.class);

        try (MockedStatic<Jsoup> jsoup = Mockito.mockStatic(Jsoup.class)) {
            jsoup.when(() -> Jsoup.connect(anyString())).thenReturn(mockConnection);
            when(mockConnection.userAgent(anyString())).thenReturn(mockConnection);
            when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);
            when(mockConnection.get()).thenReturn(mockDocument);

            // Mock price element
            when(mockDocument.selectFirst(contains("price"))).thenReturn(mockPriceElement);
            when(mockPriceElement.text()).thenReturn("$99.99");

            // Mock name element
            when(mockDocument.selectFirst(contains("productTitle"))).thenReturn(mockNameElement);
            when(mockNameElement.text()).thenReturn("Test Product");

            // Mock image element
            when(mockDocument.selectFirst(contains("mainImage"))).thenReturn(mockImageElement);
            when(mockImageElement.attr("src")).thenReturn("https://example.com/image.jpg");

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
            when(mockConnection.userAgent(anyString())).thenReturn(mockConnection);
            when(mockConnection.timeout(anyInt())).thenReturn(mockConnection);
            when(mockConnection.get()).thenThrow(new IOException("Connection failed"));

            // When
            Optional<BigDecimal> result = scraperService.scrapePrice(TEST_URL);

            // Then
            assertThat(result).isEmpty();
        }
    }
} 