package com.ds.user.application.controllers.v1;

import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.dto.auth.SyncUserRequest;
import com.ds.user.common.interfaces.IUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserController
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IUserService userService;

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
    void upsertByKeycloakId_shouldReturnUser_whenValidRequest() throws Exception {
        // Given
        SyncUserRequest request = SyncUserRequest.builder()
                .keycloakId(KEYCLOAK_ID)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .build();

        when(userService.upsertByKeycloakId(
                eq(KEYCLOAK_ID),
                eq("testuser"),
                eq("test@example.com"),
                eq("Test"),
                eq("User")
        )).thenReturn(testUser);

        // When & Then
        mockMvc.perform(post("/api/v1/users/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keycloakId").value(KEYCLOAK_ID))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void upsertByKeycloakId_shouldReturnBadRequest_whenKeycloakIdMissing() throws Exception {
        // Given
        SyncUserRequest request = SyncUserRequest.builder()
                .username("testuser")
                .email("test@example.com")
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/users/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
