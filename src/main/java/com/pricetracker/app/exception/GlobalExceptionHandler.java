package com.pricetracker.app.exception;

import com.pricetracker.app.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

/**
 * Global exception handler for centralized exception handling across the application.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handles ResourceNotFoundException, returning a 404 Not Found response.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), HttpStatus.NOT_FOUND.value()));
    }
    
    /**
     * Handles ProductAlreadyTrackedException, returning a 409 Conflict response.
     */
    @ExceptionHandler(ProductAlreadyTrackedException.class)
    public ResponseEntity<ApiResponse<Object>> handleProductAlreadyTrackedException(ProductAlreadyTrackedException ex) {
        log.warn("Product already tracked: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), HttpStatus.CONFLICT.value()));
    }
    
    /**
     * Handles ScrapingException, returning a 500 Internal Server Error or 400 Bad Request.
     */
    @ExceptionHandler(ScrapingException.class)
    public ResponseEntity<ApiResponse<Object>> handleScrapingException(ScrapingException ex) {
        log.error("Scraping error: {}", ex.getMessage(), ex);
        // Determine if it's a client error (e.g., invalid URL) or server error
        HttpStatus status = (ex.getCause() instanceof IllegalArgumentException) 
                ? HttpStatus.BAD_REQUEST 
                : HttpStatus.INTERNAL_SERVER_ERROR;
        
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(ex.getMessage(), status.value()));
    }
    
    /**
     * Handles validation exceptions (e.g., @Valid failures), returning a 400 Bad Request response.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("Validation error: {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMessage, HttpStatus.BAD_REQUEST.value()));
    }
    
    /**
     * Catch-all handler for any unhandled exceptions, returning a 500 Internal Server Error response.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }
} 