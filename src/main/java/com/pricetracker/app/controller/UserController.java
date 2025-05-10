package com.pricetracker.app.controller;

import com.pricetracker.app.dto.request.CreateUserRequest;
import com.pricetracker.app.dto.response.ApiResponse;
import com.pricetracker.app.dto.response.UserResponse;
import com.pricetracker.app.entity.User;
import com.pricetracker.app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * User Management Controller
 * 
 * Provides endpoints for managing user accounts.
 * Some operations require admin privileges.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Create new user account
     * 
     * @param request User creation request data
     * @return Created user details
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createUser(request);
        UserResponse userResponse = new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name(), user.getStatus().name());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(userResponse, "User created successfully"));
    }

    /**
     * Get all users
     * 
     * @return List of all users
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserResponse> userResponses = users.stream()
            .map(user -> new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name(), user.getStatus().name()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(userResponses, "Users retrieved successfully"));
    }
} 