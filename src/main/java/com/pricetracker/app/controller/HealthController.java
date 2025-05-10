package com.pricetracker.app.controller;

import com.pricetracker.app.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Basic health check controller.
 * Provides an endpoint to check if the API is up and running.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * Simple health check endpoint.
     * @return Status message indicating the API is operational.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<String>> checkHealth() {
        return ResponseEntity.ok(
                ApiResponse.success("Price Tracker API is operational", "Health check successful")
        );
    }
} 