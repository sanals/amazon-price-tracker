package com.pricetracker.app.notification;

import com.pricetracker.app.entity.TrackedProduct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationService {
    
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Override
    @Async("taskExecutor")
    public void sendPriceAlert(TrackedProduct trackedProduct, BigDecimal currentPrice) {
        try {
            MimeMessage message = createPriceAlertMessage(trackedProduct, currentPrice);
            mailSender.send(message);
            log.info("Price alert email sent for product {} to user {}", 
                trackedProduct.getProduct().getId(), trackedProduct.getUserId());
        } catch (MailException | MessagingException e) {
            log.error("Failed to send price alert email for product {} to user {}: {}", 
                trackedProduct.getProduct().getId(), trackedProduct.getUserId(), e.getMessage());
        }
    }
    
    private MimeMessage createPriceAlertMessage(TrackedProduct trackedProduct, BigDecimal currentPrice) 
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(getUserEmail(trackedProduct.getUserId())); 
        helper.setSubject("Price Alert: " + trackedProduct.getProduct().getName());
        
        // Create context for template
        Context context = new Context();
        context.setVariables(createTemplateVariables(trackedProduct, currentPrice));
        
        // Process template
        String htmlContent = templateEngine.process("price-alert", context);
        helper.setText(htmlContent, true);
        
        return message;
    }
    
    private Map<String, Object> createTemplateVariables(TrackedProduct trackedProduct, BigDecimal currentPrice) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("productName", trackedProduct.getProduct().getName());
        variables.put("productUrl", trackedProduct.getProduct().getProductUrl());
        variables.put("currentPrice", currentPrice);
        variables.put("desiredPrice", trackedProduct.getDesiredPrice());
        variables.put("timestamp", formatInstant(trackedProduct.getUpdatedAt()));
        return variables;
    }
    
    private String formatInstant(java.time.Instant instant) {
        return DATE_FORMATTER.format(instant.atZone(ZoneId.systemDefault()));
    }
    
    /**
     * Get the email address for a user by their ID.
     * 
     * TODO: This method should be implemented to retrieve the user's email from 
     * a UserRepository or UserService. For now, it returns a placeholder.
     * 
     * In a production environment, this would:
     * 1. Inject UserRepository or UserService
     * 2. Look up the user by ID 
     * 3. Return the user's actual email address
     * 
     * @param userId the ID of the user
     * @return the email address of the user
     */
    private String getUserEmail(Long userId) {
        // TODO: Replace with actual implementation that retrieves email from user repository
        return "user@example.com";
    }
} 