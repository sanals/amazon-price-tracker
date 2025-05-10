package com.pricetracker.app.repository;

import com.pricetracker.app.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void whenSaveUser_thenUserIsSaved() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedpassword");

        // When
        User savedUser = userRepository.save(user);

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void whenFindById_thenUserIsFound() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedpassword");
        User savedUser = entityManager.persist(user);
        entityManager.flush();

        // When
        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void whenFindByUsername_thenUserIsFound() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedpassword");
        entityManager.persist(user);
        entityManager.flush();

        // When
        Optional<User> foundUser = userRepository.findByUsername("testuser");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void whenFindByEmail_thenUserIsFound() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedpassword");
        entityManager.persist(user);
        entityManager.flush();

        // When
        Optional<User> foundUser = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void whenExistsByUsername_thenReturnsTrue() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedpassword");
        entityManager.persist(user);
        entityManager.flush();

        // When
        Boolean exists = userRepository.existsByUsername("testuser");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void whenExistsByEmail_thenReturnsTrue() {
        // Given
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashedpassword");
        entityManager.persist(user);
        entityManager.flush();

        // When
        Boolean exists = userRepository.existsByEmail("test@example.com");

        // Then
        assertThat(exists).isTrue();
    }
} 