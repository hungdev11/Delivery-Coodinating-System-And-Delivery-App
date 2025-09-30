package com.ds.user;

import com.ds.user.app_context.models.User;
import com.ds.user.app_context.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test for User Service API
 * Tests all API endpoints end-to-end
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID createdUserId;

    @BeforeAll
    void setup() {
        // Clean up test data
        userRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Should create a new user")
    void testCreateUser() throws Exception {
        // Given
        User newUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .phone("0123456789")
                .address("123 Test Street")
                .status(User.UserStatus.ACTIVE)
                .build();

        // When & Then
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        // Extract created user ID for subsequent tests
        String responseBody = result.getResponse().getContentAsString();
        User createdUser = objectMapper.readValue(responseBody, User.class);
        createdUserId = createdUser.getId();

        assertThat(createdUserId).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("Should get user by ID")
    void testGetUserById() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + createdUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdUserId.toString()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @Order(3)
    @DisplayName("Should list all users")
    void testListAllUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("testuser"));
    }

    @Test
    @Order(4)
    @DisplayName("Should update user")
    void testUpdateUser() throws Exception {
        // Given
        User updateData = User.builder()
                .firstName("Updated")
                .lastName("Name")
                .email("updated@example.com")
                .phone("0987654321")
                .address("456 Updated Street")
                .build();

        // When & Then
        mockMvc.perform(put("/api/v1/users/" + createdUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("Name"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.phone").value("0987654321"));
    }

    @Test
    @Order(5)
    @DisplayName("Should delete user")
    void testDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/v1/users/" + createdUserId))
                .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/v1/users/" + createdUserId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(6)
    @DisplayName("Should return 404 for non-existent user")
    void testGetNonExistentUser() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/users/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    @DisplayName("Should create multiple users and list them")
    void testCreateMultipleUsersAndList() throws Exception {
        // Create user 1
        User user1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .firstName("User")
                .lastName("One")
                .status(User.UserStatus.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isOk());

        // Create user 2
        User user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .firstName("User")
                .lastName("Two")
                .status(User.UserStatus.PENDING)
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isOk());

        // List all users
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").exists())
                .andExpect(jsonPath("$[1].username").exists());
    }

    @AfterAll
    void cleanup() {
        // Clean up test data
        userRepository.deleteAll();
    }
}
