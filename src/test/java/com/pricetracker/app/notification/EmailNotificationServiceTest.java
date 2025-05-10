package com.pricetracker.app.notification;

import com.pricetracker.app.entity.Product;
import com.pricetracker.app.entity.TrackedProduct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;
    
    @Mock
    private TemplateEngine templateEngine;
    
    @Mock
    private MimeMessage mimeMessage;
    
    @InjectMocks
    private EmailNotificationService emailNotificationService;
    
    private TrackedProduct testTrackedProduct;
    private Product testProduct;
    
    @BeforeEach
    void setUp() throws MessagingException {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setProductUrl("https://example.com/product");
        testProduct.setLastCheckedPrice(new BigDecimal("99.99"));
        
        testTrackedProduct = new TrackedProduct();
        testTrackedProduct.setId(1L);
        testTrackedProduct.setUserId(1L);
        testTrackedProduct.setProduct(testProduct);
        testTrackedProduct.setDesiredPrice(new BigDecimal("89.99"));
        testTrackedProduct.setNotificationEnabled(true);
        testTrackedProduct.setUpdatedAt(Instant.now());
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Test</html>");
    }
    
    @Test
    void whenSendPriceAlert_thenSendEmail() throws MessagingException {
        // Given
        BigDecimal currentPrice = new BigDecimal("79.99");
        
        // When
        emailNotificationService.sendPriceAlert(testTrackedProduct, currentPrice);
        
        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
        verify(templateEngine).process(eq("price-alert"), any(Context.class));
    }
    
    @Test
    void whenSendPriceAlert_withMailError_thenLogError() throws MessagingException {
        // Given
        BigDecimal currentPrice = new BigDecimal("79.99");
        doThrow(new MailSendException("Failed to send")).when(mailSender).send(any(MimeMessage.class));
        
        // When
        emailNotificationService.sendPriceAlert(testTrackedProduct, currentPrice);
        
        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
        verify(templateEngine).process(eq("price-alert"), any(Context.class));
    }
} 