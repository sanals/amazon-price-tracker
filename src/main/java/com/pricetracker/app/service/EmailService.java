package com.pricetracker.app.service;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
} 