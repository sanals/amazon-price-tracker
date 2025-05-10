package com.pricetracker.app.service;

import com.pricetracker.app.dto.request.CreateUserRequest;
import com.pricetracker.app.entity.User;
import java.util.List;

public interface UserService {
    User createUser(CreateUserRequest request);
    User updateUser(User user);
    List<User> getAllUsers();
    User getUserById(Long id);
    void deleteUser(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
} 