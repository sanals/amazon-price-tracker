package com.pricetracker.app.repository;

import com.pricetracker.app.entity.RefreshToken;
import com.pricetracker.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.user = ?1")
    void deleteByUser(User user);
} 