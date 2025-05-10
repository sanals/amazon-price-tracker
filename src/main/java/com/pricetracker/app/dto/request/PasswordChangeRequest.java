package com.pricetracker.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PasswordChangeRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$", 
            message = "Password must be at least 8 characters long and contain both letters and numbers")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
} 