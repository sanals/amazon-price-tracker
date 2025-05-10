package com.pricetracker.app.util;

import java.security.SecureRandom;
import java.util.Base64;

public class KeyGenerator {
    public static void main(String[] args) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[64]; // 512 bits
        random.nextBytes(bytes);
        String encodedKey = Base64.getEncoder().encodeToString(bytes);
        System.out.println("Generated JWT Secret Key: " + encodedKey);
    }
} 