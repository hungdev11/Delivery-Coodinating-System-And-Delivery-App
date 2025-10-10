package com.ds.user.business.v1.services;

import com.ds.user.app_context.models.User;
import com.ds.user.app_context.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private static final String KEYCLOAK_ID = "keycloak-123";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .keycloakId(KEYCLOAK_ID)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .status(User.UserStatus.ACTIVE)
                .build();
    }

    @Test
    void upsertByKeycloakId_shouldCreateNewUser_whenUserDoesNotExist() {
        // Given
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.upsertByKeycloakId(
                KEYCLOAK_ID,
                "testuser",
                "test@example.com",
                "Test",
                "User"
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getKeycloakId()).isEqualTo(KEYCLOAK_ID);
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        
        verify(userRepository).findByKeycloakId(KEYCLOAK_ID);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void upsertByKeycloakId_shouldUpdateUser_whenUserExists() {
        // Given
        User existingUser = User.builder()
                .keycloakId(KEYCLOAK_ID)
                .username("oldusername")
                .email("old@example.com")
                .firstName("Old")
                .lastName("Name")
                .status(User.UserStatus.ACTIVE)
                .build();

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        User result = userService.upsertByKeycloakId(
                KEYCLOAK_ID,
                "newusername",
                "new@example.com",
                "New",
                "Name"
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getKeycloakId()).isEqualTo(KEYCLOAK_ID);
        assertThat(result.getUsername()).isEqualTo("newusername");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getFirstName()).isEqualTo("New");
        assertThat(result.getLastName()).isEqualTo("Name");
        
        verify(userRepository).findByKeycloakId(KEYCLOAK_ID);
        verify(userRepository).save(existingUser);
    }

    @Test
    void upsertByKeycloakId_shouldKeepExistingFields_whenNullProvided() {
        // Given
        User existingUser = User.builder()
                .keycloakId(KEYCLOAK_ID)
                .username("existinguser")
                .email("existing@example.com")
                .firstName("Existing")
                .lastName("User")
                .status(User.UserStatus.ACTIVE)
                .build();

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        User result = userService.upsertByKeycloakId(
                KEYCLOAK_ID,
                null,  // Don't update username
                "new@example.com",
                null,  // Don't update firstName
                "NewLastName"
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("existinguser"); // Kept
        assertThat(result.getEmail()).isEqualTo("new@example.com"); // Updated
        assertThat(result.getFirstName()).isEqualTo("Existing"); // Kept
        assertThat(result.getLastName()).isEqualTo("NewLastName"); // Updated
        
        verify(userRepository).findByKeycloakId(KEYCLOAK_ID);
        verify(userRepository).save(existingUser);
    }
}
