package com.pricetracker.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Login Request DTO
 * 
 * Contains user credentials for authentication
 */
@Data
public class LoginRequest {
    @NotBlank(message = "Username is required")
    private String username;
    
    @NotBlank(message = "Password is required")
    private String password;
} 