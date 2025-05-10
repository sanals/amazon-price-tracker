package com.pricetracker.app.repository;

import com.pricetracker.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find a user by their username.
     * 
     * @param username the username to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Find a user by their email.
     * 
     * @param email the email to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Check if a user exists with the given username.
     * 
     * @param username the username to check
     * @return true if a user exists with the given username
     */
    Boolean existsByUsername(String username);
    
    /**
     * Check if a user exists with the given email.
     * 
     * @param email the email to check
     * @return true if a user exists with the given email
     */
    Boolean existsByEmail(String email);
} 