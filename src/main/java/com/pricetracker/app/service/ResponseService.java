package com.pricetracker.app.service;

import java.util.List;
import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.pricetracker.app.dto.response.ApiResponse;
import com.pricetracker.app.dto.response.ProductResponse;

/**
 * Service for standardizing API responses across the application
 */
@Service
public class ResponseService {

    /**
     * Standardize single item responses
     * 
     * @param <T>     Type of data
     * @param data    Data to be returned
     * @param message Success message
     * @return Standardized API response with single item
     */
    public <T> ApiResponse<T> createSingleResponse(T data, String message) {
        return ApiResponse.success(data, message);
    }

    /**
     * Standardize created item responses with 201 status
     * 
     * @param <T>     Type of data
     * @param data    Data to be returned
     * @param message Success message
     * @return Standardized API response with created status
     */
    public <T> ApiResponse<T> createCreatedResponse(T data, String message) {
        return new ApiResponse<>(
            ApiResponse.Status.SUCCESS,
            HttpStatus.CREATED.value(),
            message,
            data,
            Instant.now()
        );
    }

    /**
     * Standardize list responses by wrapping them in a Page
     * 
     * @param <T>      Type of list items
     * @param data     List of items
     * @param pageable Pagination information
     * @param message  Success message
     * @return Standardized API response with paged data
     */
    public <T> ApiResponse<Page<T>> createPageResponse(List<T> data, Pageable pageable, String message) {
        Page<T> page = new PageImpl<>(data, pageable, data.size());
        return ApiResponse.success(page, message);
    }

    /**
     * Standardize existing page responses
     * 
     * @param <T>     Type of page items
     * @param page    Page of items
     * @param message Success message
     * @return Standardized API response with paged data
     */
    public <T> ApiResponse<Page<T>> createPageResponse(Page<T> page, String message) {
        return ApiResponse.success(page, message);
    }

    /**
     * Standardize empty success responses
     * 
     * @param message Success message
     * @return Standardized API response with no data
     */
    public ApiResponse<Void> createEmptyResponse(String message) {
        return ApiResponse.success(null, message);
    }

    /**
     * Generic success response with default message
     * 
     * @param <T>  Type of data
     * @param data Data to be returned
     * @return Standardized API response with success status
     */
    public <T> ApiResponse<T> success(T data) {
        return ApiResponse.success(data);
    }

    /**
     * Success response specifically for ProductResponse
     * 
     * @param product ProductResponse to be returned
     * @return Standardized API response with ProductResponse
     */
    public ApiResponse<ProductResponse> success(ProductResponse product) {
        return ApiResponse.success(product, "Product operation successful");
    }

    /**
     * Success response specifically for Page of ProductResponse
     * 
     * @param products Page of ProductResponse to be returned
     * @return Standardized API response with Page of ProductResponse
     */
    public ApiResponse<Page<ProductResponse>> success(Page<ProductResponse> products) {
        return ApiResponse.success(products, "Products retrieved successfully");
    }
}