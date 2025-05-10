package com.pricetracker.app.controller;

import com.pricetracker.app.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Health", description = "API health check operations")
public class HealthController {

    /**
     * Simple health check endpoint.
     * @return Status message indicating the API is operational.
     */
    @GetMapping
    @Operation(summary = "Check API health", description = "Returns a status message if the API is operational")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "API is operational",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ApiResponse.class))
        )
    })
    public ResponseEntity<ApiResponse<String>> checkHealth() {
        return ResponseEntity.ok(
                ApiResponse.success("Price Tracker API is operational", "Health check successful")
        );
    }
} 