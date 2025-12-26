package com.ds.communication_service.application.controllers.v1;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ds.communication_service.app_context.repositories.TicketRepository;
import com.ds.communication_service.common.dto.BaseResponse;
import com.ds.communication_service.common.dto.BulkQueryRequest;
import com.ds.communication_service.common.dto.CreateTicketRequest;
import com.ds.communication_service.common.dto.PageResponse;
import com.ds.communication_service.common.dto.TicketResponse;
import com.ds.communication_service.common.dto.UpdateTicketRequest;
import com.ds.communication_service.common.enums.TicketStatus;
import com.ds.communication_service.common.enums.TicketType;
import com.ds.communication_service.common.interfaces.ITicketService;

/**
 * Unit tests for TicketController
 * Tests controller methods directly without running server
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TicketController Tests")
class TicketControllerTest {

    @Mock
    private ITicketService ticketService;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketController ticketController;

    private CreateTicketRequest createRequest;
    private TicketResponse ticketResponse;
    private UUID ticketId;
    private String reporterId;
    private String adminId;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
        reporterId = "reporter-123";
        adminId = "admin-123";

        createRequest = CreateTicketRequest.builder()
                .type(TicketType.DELIVERY_FAILED)
                .parcelId("parcel-123")
                .description("Delivery failed")
                .build();

        ticketResponse = TicketResponse.builder()
                .id(ticketId)
                .type(TicketType.DELIVERY_FAILED)
                .status(TicketStatus.OPEN)
                .parcelId("parcel-123")
                .reporterId(reporterId)
                .description("Delivery failed")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("createTicket Tests")
    class CreateTicketTests {

        @Test
        @DisplayName("Should create ticket successfully")
        void shouldCreateTicketSuccessfully() {
            // Arrange
            when(ticketService.createTicket(any(CreateTicketRequest.class), eq(reporterId)))
                    .thenReturn(ticketResponse);

            // Act
            ResponseEntity<BaseResponse<TicketResponse>> response = 
                    ticketController.createTicket(createRequest, reporterId);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getResult());
            assertEquals(ticketId, response.getBody().getResult().getId());
            assertEquals("Ticket created successfully", response.getBody().getMessage());
            verify(ticketService).createTicket(createRequest, reporterId);
        }
    }

    @Nested
    @DisplayName("getTicketById Tests")
    class GetTicketByIdTests {

        @Test
        @DisplayName("Should get ticket by ID successfully")
        void shouldGetTicketByIdSuccessfully() {
            // Arrange
            when(ticketService.getTicketById(ticketId)).thenReturn(ticketResponse);

            // Act
            ResponseEntity<BaseResponse<TicketResponse>> response = 
                    ticketController.getTicketById(ticketId);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getResult());
            assertEquals(ticketId, response.getBody().getResult().getId());
            verify(ticketService).getTicketById(ticketId);
        }
    }

    @Nested
    @DisplayName("updateTicket Tests")
    class UpdateTicketTests {

        @Test
        @DisplayName("Should update ticket successfully")
        void shouldUpdateTicketSuccessfully() {
            // Arrange
            UpdateTicketRequest updateRequest = UpdateTicketRequest.builder()
                    .status(TicketStatus.RESOLVED)
                    .resolutionNotes("Resolved")
                    .actionTaken("RESOLVE")
                    .build();

            TicketResponse updatedResponse = TicketResponse.builder()
                    .id(ticketId)
                    .status(TicketStatus.RESOLVED)
                    .assignedAdminId(adminId)
                    .resolutionNotes("Resolved")
                    .actionTaken("RESOLVE")
                    .build();

            when(ticketService.updateTicket(eq(ticketId), any(UpdateTicketRequest.class), eq(adminId)))
                    .thenReturn(updatedResponse);

            // Act
            ResponseEntity<BaseResponse<TicketResponse>> response = 
                    ticketController.updateTicket(ticketId, updateRequest, adminId);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(TicketStatus.RESOLVED, response.getBody().getResult().getStatus());
            assertEquals("Ticket updated successfully", response.getBody().getMessage());
            verify(ticketService).updateTicket(ticketId, updateRequest, adminId);
        }
    }

    @Nested
    @DisplayName("Bulk Query Tests")
    class BulkQueryTests {

        @Test
        @DisplayName("Should get tickets by list of IDs")
        void shouldGetTicketsByIds() {
            // Arrange
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            BulkQueryRequest request = BulkQueryRequest.builder()
                    .ids(List.of(id1.toString(), id2.toString()))
                    .build();

            TicketResponse response1 = TicketResponse.builder().id(id1).build();
            TicketResponse response2 = TicketResponse.builder().id(id2).build();

            when(ticketService.getTicketsByIds(anyList())).thenReturn(List.of(response1, response2));

            // Act
            ResponseEntity<BaseResponse<List<TicketResponse>>> response = 
                    ticketController.getTicketsByIds(request);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getResult());
            assertEquals(2, response.getBody().getResult().size());
            verify(ticketService).getTicketsByIds(anyList());
        }

        @Test
        @DisplayName("Should get tickets by list of parcel IDs")
        void shouldGetTicketsByParcelIds() {
            // Arrange
            BulkQueryRequest request = BulkQueryRequest.builder()
                    .ids(List.of("parcel-1", "parcel-2"))
                    .build();

            TicketResponse response1 = TicketResponse.builder().parcelId("parcel-1").build();
            TicketResponse response2 = TicketResponse.builder().parcelId("parcel-2").build();

            when(ticketService.getTicketsByParcelIds(anyList())).thenReturn(List.of(response1, response2));

            // Act
            ResponseEntity<BaseResponse<List<TicketResponse>>> response = 
                    ticketController.getTicketsByParcelIds(request);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(2, response.getBody().getResult().size());
            verify(ticketService).getTicketsByParcelIds(anyList());
        }

        @Test
        @DisplayName("Should get tickets by list of assignment IDs")
        void shouldGetTicketsByAssignmentIds() {
            // Arrange
            BulkQueryRequest request = BulkQueryRequest.builder()
                    .ids(List.of("assignment-1", "assignment-2"))
                    .build();

            when(ticketService.getTicketsByAssignmentIds(anyList())).thenReturn(List.of());

            // Act
            ResponseEntity<BaseResponse<List<TicketResponse>>> response = 
                    ticketController.getTicketsByAssignmentIds(request);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(ticketService).getTicketsByAssignmentIds(anyList());
        }
    }

    @Nested
    @DisplayName("Filter Tests")
    class FilterTests {

        @Test
        @DisplayName("Should get tickets with status filter")
        void shouldGetTicketsWithStatusFilter() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<TicketResponse> ticketPage = new PageImpl<>(List.of(ticketResponse), pageable, 1);

            when(ticketService.getTicketsByStatus(TicketStatus.OPEN, pageable)).thenReturn(ticketPage);

            // Act
            ResponseEntity<BaseResponse<PageResponse<TicketResponse>>> response = 
                    ticketController.getTickets(TicketStatus.OPEN, null, pageable);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getResult());
            assertEquals(1, response.getBody().getResult().totalElements());
            verify(ticketService).getTicketsByStatus(TicketStatus.OPEN, pageable);
        }

        @Test
        @DisplayName("Should get open tickets")
        void shouldGetOpenTickets() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<TicketResponse> ticketPage = new PageImpl<>(List.of(ticketResponse), pageable, 1);

            when(ticketService.getOpenTickets(pageable)).thenReturn(ticketPage);

            // Act
            ResponseEntity<BaseResponse<PageResponse<TicketResponse>>> response = 
                    ticketController.getOpenTickets(pageable);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(ticketService).getOpenTickets(pageable);
        }
    }
}
