package com.pricetracker.app.service.impl;

import com.pricetracker.app.dto.request.LoginRequest;
import com.pricetracker.app.dto.request.PasswordChangeRequest;
import com.pricetracker.app.dto.request.TokenRefreshRequest;
import com.pricetracker.app.dto.response.AuthResponse;
import com.pricetracker.app.dto.response.TokenRefreshResponse;
import com.pricetracker.app.entity.PasswordResetToken;
import com.pricetracker.app.entity.RefreshToken;
import com.pricetracker.app.entity.User;
import com.pricetracker.app.exception.TokenRefreshException;
import com.pricetracker.app.repository.PasswordResetTokenRepository;
import com.pricetracker.app.repository.UserRepository;
import com.pricetracker.app.security.JwtService;
import com.pricetracker.app.service.AuthService;
import com.pricetracker.app.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Instead of casting, fetch the user from the repository
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found with username: " + request.getUsername()));
        
        // Update last login time
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        
        String jwt = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername());
        
        return AuthResponse.builder()
            .token(jwt)
            .refreshToken(refreshToken.getToken())
            .username(user.getUsername())
            .role(user.getRole().name())
            .build();
    }

    @Override
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        // If using database storage, find in database and verify
        if (refreshTokenService.isUsingDatabaseStorage()) {
            return refreshTokenService.findByToken(requestRefreshToken)
                    .map(refreshTokenService::verifyExpiration)
                    .map(RefreshToken::getUser)
                    .map(user -> {
                        String token = jwtService.generateToken(user);
                        return TokenRefreshResponse.builder()
                                .accessToken(token)
                                .refreshToken(requestRefreshToken)
                                .build();
                    })
                    .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "Refresh token is not in database!"));
        } else {
            // For JWT-based refresh tokens, validate the token directly
            try {
                // Will throw exception if token is invalid
                String username = refreshTokenService.validateRefreshToken(requestRefreshToken);
                
                // Get user from username
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
                
                // Generate new access token
                String newAccessToken = jwtService.generateToken(user);
                
                // Return response with same refresh token
                return TokenRefreshResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(requestRefreshToken) // Keep the same refresh token
                        .build();
            } catch (TokenRefreshException e) {
                throw e;
            } catch (Exception e) {
                throw new TokenRefreshException(requestRefreshToken, "Failed to refresh token: " + e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        refreshTokenService.deleteByUserId(user.getId());
    }

    @Override
    @Transactional
    public void changePassword(String username, PasswordChangeRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        
        // Verify new password and confirm password match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Logout all devices (optional)
        refreshTokenService.deleteByUserId(user.getId());
    }

    @Override
    @Transactional
    public void sendPasswordResetEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        // Delete any existing token
        passwordResetTokenRepository.findByUser(user).ifPresent(passwordResetTokenRepository::delete);
        
        // Create new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .build();
        
        passwordResetTokenRepository.save(resetToken);
        
        // Send email - for now just log it since email service is not yet implemented
        String resetUrl = baseUrl + "/auth/reset-password?token=" + token;
        String subject = "Password Reset Request";
        String body = "Hello " + user.getUsername() + ",\n\n" +
                "You requested to reset your password. Please click the link below to reset your password:\n\n" +
                resetUrl + "\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you did not request a password reset, please ignore this email.";
        
        // Log instead of sending email (since emailService is not implemented yet)
        logger.info("Would send password reset email to: {} with token: {}", user.getEmail(), token);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid password reset token"));
        
        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new RuntimeException("Password reset token has expired");
        }
        
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Delete token after use
        passwordResetTokenRepository.delete(resetToken);
        
        // Logout all devices
        refreshTokenService.deleteByUserId(user.getId());
    }
} 