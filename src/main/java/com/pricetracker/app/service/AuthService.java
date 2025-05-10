package com.pricetracker.app.service;

import com.pricetracker.app.dto.request.LoginRequest;
import com.pricetracker.app.dto.request.PasswordChangeRequest;
import com.pricetracker.app.dto.request.TokenRefreshRequest;
import com.pricetracker.app.dto.response.AuthResponse;
import com.pricetracker.app.dto.response.TokenRefreshResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    TokenRefreshResponse refreshToken(TokenRefreshRequest request);
    void logout(String username);
    void changePassword(String username, PasswordChangeRequest request);
    void sendPasswordResetEmail(String email);
    void resetPassword(String token, String newPassword);
} 