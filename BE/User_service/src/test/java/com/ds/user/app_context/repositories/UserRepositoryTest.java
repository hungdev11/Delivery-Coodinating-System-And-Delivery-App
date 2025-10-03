package com.ds.user.app_context.repositories;

import com.ds.user.app_context.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository Test for UserRepository
 * Tests database operations
 */
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .phone("0123456789")
                .keycloakId("keycloak-123")
                .status(User.UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve user")
    void testSaveAndRetrieveUser() {
        // When
        User savedUser = userRepository.save(testUser);
        entityManager.flush();

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find user by username")
    void testFindByUsername() {
        // Given
        userRepository.save(testUser);
        entityManager.flush();

        // When
        Optional<User> found = userRepository.findByUsername("testuser");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("testuser");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should find user by email")
    void testFindByEmail() {
        // Given
        userRepository.save(testUser);
        entityManager.flush();

        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should find user by phone")
    void testFindByPhone() {
        // Given
        userRepository.save(testUser);
        entityManager.flush();

        // When
        Optional<User> found = userRepository.findByPhone("0123456789");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getPhone()).isEqualTo("0123456789");
    }

    @Test
    @DisplayName("Should find user by Keycloak ID")
    void testFindByKeycloakId() {
        // Given
        userRepository.save(testUser);
        entityManager.flush();

        // When
        Optional<User> found = userRepository.findByKeycloakId("keycloak-123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getKeycloakId()).isEqualTo("keycloak-123");
    }

    @Test
    @DisplayName("Should check if phone exists")
    void testExistsByPhone() {
        // Given
        userRepository.save(testUser);
        entityManager.flush();

        // When
        boolean exists = userRepository.existsByPhone("0123456789");
        boolean notExists = userRepository.existsByPhone("9999999999");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should find all users")
    void testFindAll() {
        // Given
        User user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .phone("0987654321")
                .status(User.UserStatus.PENDING)
                .build();

        userRepository.save(testUser);
        userRepository.save(user2);
        entityManager.flush();

        // When
        List<User> users = userRepository.findAll();

        // Then
        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getUsername)
                .containsExactlyInAnyOrder("testuser", "user2");
    }

    @Test
    @DisplayName("Should delete user")
    void testDeleteUser() {
        // Given
        User savedUser = userRepository.save(testUser);
        entityManager.flush();

        // When
        userRepository.deleteById(savedUser.getId());
        entityManager.flush();

        // Then
        Optional<User> deleted = userRepository.findById(savedUser.getId());
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when user not found")
    void testFindByUsernameNotFound() {
        // When
        Optional<User> found = userRepository.findByUsername("nonexistent");

        // Then
        assertThat(found).isEmpty();
    }
}
