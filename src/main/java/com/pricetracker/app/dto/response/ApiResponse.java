package com.pricetracker.app.dto.response;

import java.time.Instant;

/**
 * Standard API response wrapper for all endpoints.
 * @param <T> The type of data being returned
 */
public record ApiResponse<T>(
    Status status,
    int code,
    String message,
    T data,
    Instant timestamp
) {
    public enum Status {
        SUCCESS,
        ERROR
    }

    /**
     * Create a successful response with data and message.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(
            Status.SUCCESS,
            200,
            message,
            data,
            Instant.now()
        );
    }

    /**
     * Create a successful response with data only.
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operation successful");
    }

    /**
     * Create an error response with message and code.
     */
    public static <T> ApiResponse<T> error(String message, int code) {
        return new ApiResponse<>(
            Status.ERROR,
            code,
            message,
            null,
            Instant.now()
        );
    }

    /**
     * Create an error response with message (default code 400).
     */
    public static <T> ApiResponse<T> error(String message) {
        return error(message, 400);
    }
} 