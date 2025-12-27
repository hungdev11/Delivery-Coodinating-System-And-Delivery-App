package com.ds.communication_service.business.v1.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

import com.ds.communication_service.app_context.models.Conversation;
import com.ds.communication_service.app_context.models.Ticket;
import com.ds.communication_service.app_context.repositories.InteractiveProposalRepository;
import com.ds.communication_service.app_context.repositories.MessageRepository;
import com.ds.communication_service.app_context.repositories.TicketRepository;
import com.ds.communication_service.common.dto.CreateTicketRequest;
import com.ds.communication_service.common.dto.TicketResponse;
import com.ds.communication_service.common.dto.UpdateTicketRequest;
import com.ds.communication_service.common.enums.TicketStatus;
import com.ds.communication_service.common.enums.TicketType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

/**
 * Unit tests for TicketService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService Tests")
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ConversationService conversationService;

    @Mock
    private InteractiveProposalRepository proposalRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @InjectMocks
    private TicketService ticketService;

    private Ticket ticket;
    private UUID ticketId;
    private String parcelId;
    private String reporterId;
    private String adminId;

    @BeforeEach
    void setUp() {
        ticketId = UUID.randomUUID();
        parcelId = "parcel-123";
        reporterId = "reporter-123";
        adminId = "admin-123";

        ticket = new Ticket();
        ticket.setId(ticketId);
        ticket.setType(TicketType.DELIVERY_FAILED);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setParcelId(parcelId);
        ticket.setReporterId(reporterId);
        ticket.setDescription("Delivery failed - recipient not available");
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("createTicket Tests")
    class CreateTicketTests {

        @Test
        @DisplayName("Should create ticket successfully")
        void shouldCreateTicketSuccessfully() {
            // Arrange
            CreateTicketRequest request = CreateTicketRequest.builder()
                    .type(TicketType.DELIVERY_FAILED)
                    .parcelId(parcelId)
                    .assignmentId("assignment-123")
                    .description("Delivery failed")
                    .build();

            when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
                Ticket saved = invocation.getArgument(0);
                saved.setId(ticketId);
                saved.setCreatedAt(LocalDateTime.now());
                saved.setUpdatedAt(LocalDateTime.now());
                return saved;
            });

            // Mock KafkaTemplate (to avoid NPE when publishing notifications)
            // KafkaTemplate.send() returns CompletableFuture, not void
            lenient().when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
            // Mock ObjectMapper - return real ObjectNode (not null) - lenient because not called in createTicket
            lenient().when(objectMapper.createObjectNode()).thenAnswer(invocation -> 
                new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
            );

            // Act
            TicketResponse response = ticketService.createTicket(request, reporterId);

            // Assert
            assertNotNull(response);
            assertEquals(TicketType.DELIVERY_FAILED, response.getType());
            assertEquals(TicketStatus.OPEN, response.getStatus());
            assertEquals(parcelId, response.getParcelId());
            assertEquals(reporterId, response.getReporterId());
            assertEquals("Delivery failed", response.getDescription());
            verify(ticketRepository).save(any(Ticket.class));
        }
    }

    @Nested
    @DisplayName("getTicketById Tests")
    class GetTicketByIdTests {

        @Test
        @DisplayName("Should get ticket by ID successfully")
        void shouldGetTicketByIdSuccessfully() {
            // Arrange
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

            // Act
            TicketResponse response = ticketService.getTicketById(ticketId);

            // Assert
            assertNotNull(response);
            assertEquals(ticketId, response.getId());
            assertEquals(TicketType.DELIVERY_FAILED, response.getType());
            verify(ticketRepository).findById(ticketId);
        }

        @Test
        @DisplayName("Should throw exception when ticket not found")
        void shouldThrowExceptionWhenTicketNotFound() {
            // Arrange
            when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> {
                ticketService.getTicketById(ticketId);
            });
        }
    }

    @Nested
    @DisplayName("updateTicket Tests")
    class UpdateTicketTests {

        @Test
        @DisplayName("Should update ticket status to RESOLVED")
        void shouldUpdateTicketStatusToResolved() {
            // Arrange
            UpdateTicketRequest request = UpdateTicketRequest.builder()
                    .status(TicketStatus.RESOLVED)
                    .resolutionNotes("Issue resolved")
                    .actionTaken("RESOLVE")
                    .build();

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
            
            // Mock KafkaTemplate (returns CompletableFuture)
            lenient().when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
            // Mock ObjectMapper for proposal creation (when admin assigns ticket) - lenient because may not be called
            lenient().when(objectMapper.createObjectNode()).thenAnswer(invocation -> 
                new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
            );
            // Mock ConversationService to avoid NPE - lenient because only called when admin assigns
            Conversation mockConversation = new Conversation();
            mockConversation.setId(UUID.randomUUID());
            mockConversation.setUser1Id("user1");
            mockConversation.setUser2Id("user2");
            lenient().when(conversationService.findOrCreateConversation(anyString(), anyString()))
                .thenReturn(mockConversation);
            // Mock repositories to avoid NPE - lenient because only called when admin assigns
            lenient().when(proposalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            lenient().when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            lenient().doNothing().when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());

            // Act
            TicketResponse response = ticketService.updateTicket(ticketId, request, adminId);

            // Assert
            assertNotNull(response);
            assertEquals(TicketStatus.RESOLVED, response.getStatus());
            assertEquals(adminId, response.getAssignedAdminId());
            assertEquals("Issue resolved", response.getResolutionNotes());
            assertEquals("RESOLVE", response.getActionTaken());
            assertNotNull(response.getResolvedAt());
            verify(ticketRepository).save(any(Ticket.class));
        }

        @Test
        @DisplayName("Should reassign ticket to new assignment")
        void shouldReassignTicketToNewAssignment() {
            // Arrange
            String newAssignmentId = "assignment-456";
            UpdateTicketRequest request = UpdateTicketRequest.builder()
                    .status(TicketStatus.IN_PROGRESS)
                    .newAssignmentId(newAssignmentId)
                    .build();

            when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
            when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
            
            // Mock KafkaTemplate (returns CompletableFuture)
            lenient().when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
            // Mock ObjectMapper for proposal creation (when admin assigns ticket) - lenient because may not be called
            lenient().when(objectMapper.createObjectNode()).thenAnswer(invocation -> 
                new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()
            );
            // Mock ConversationService to avoid NPE - lenient because only called when admin assigns
            Conversation mockConversation = new Conversation();
            mockConversation.setId(UUID.randomUUID());
            mockConversation.setUser1Id("user1");
            mockConversation.setUser2Id("user2");
            lenient().when(conversationService.findOrCreateConversation(anyString(), anyString()))
                .thenReturn(mockConversation);
            // Mock repositories to avoid NPE - lenient because only called when admin assigns
            lenient().when(proposalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            lenient().when(messageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            lenient().doNothing().when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());

            // Act
            TicketResponse response = ticketService.updateTicket(ticketId, request, adminId);

            // Assert
            assertNotNull(response);
            assertEquals(newAssignmentId, response.getAssignmentId());
            assertEquals("REASSIGN", response.getActionTaken());
            verify(ticketRepository).save(any(Ticket.class));
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
            List<UUID> ids = List.of(id1, id2);

            Ticket ticket1 = new Ticket();
            ticket1.setId(id1);
            ticket1.setType(TicketType.DELIVERY_FAILED);
            ticket1.setStatus(TicketStatus.OPEN);

            Ticket ticket2 = new Ticket();
            ticket2.setId(id2);
            ticket2.setType(TicketType.NOT_RECEIVED);
            ticket2.setStatus(TicketStatus.OPEN);

            when(ticketRepository.findByIdIn(ids)).thenReturn(List.of(ticket1, ticket2));

            // Act
            List<TicketResponse> responses = ticketService.getTicketsByIds(ids);

            // Assert
            assertNotNull(responses);
            assertEquals(2, responses.size());
            verify(ticketRepository).findByIdIn(ids);
        }

        @Test
        @DisplayName("Should get tickets by list of parcel IDs")
        void shouldGetTicketsByParcelIds() {
            // Arrange
            List<String> parcelIds = List.of("parcel-1", "parcel-2", "parcel-3");
            
            Ticket ticket1 = new Ticket();
            ticket1.setId(UUID.randomUUID());
            ticket1.setParcelId("parcel-1");

            Ticket ticket2 = new Ticket();
            ticket2.setId(UUID.randomUUID());
            ticket2.setParcelId("parcel-2");

            when(ticketRepository.findByParcelIdIn(parcelIds)).thenReturn(List.of(ticket1, ticket2));

            // Act
            List<TicketResponse> responses = ticketService.getTicketsByParcelIds(parcelIds);

            // Assert
            assertNotNull(responses);
            assertEquals(2, responses.size());
            verify(ticketRepository).findByParcelIdIn(parcelIds);
        }

        @Test
        @DisplayName("Should return empty list when IDs list is empty")
        void shouldReturnEmptyListWhenIdsEmpty() {
            // Act
            List<TicketResponse> responses = ticketService.getTicketsByIds(List.of());

            // Assert
            assertNotNull(responses);
            assertTrue(responses.isEmpty());
            verify(ticketRepository, never()).findByIdIn(any());
        }

        @Test
        @DisplayName("Should get tickets by list of assignment IDs")
        void shouldGetTicketsByAssignmentIds() {
            // Arrange
            List<String> assignmentIds = List.of("assignment-1", "assignment-2");
            
            Ticket ticket1 = new Ticket();
            ticket1.setId(UUID.randomUUID());
            ticket1.setAssignmentId("assignment-1");

            when(ticketRepository.findByAssignmentIdIn(assignmentIds)).thenReturn(List.of(ticket1));

            // Act
            List<TicketResponse> responses = ticketService.getTicketsByAssignmentIds(assignmentIds);

            // Assert
            assertNotNull(responses);
            assertEquals(1, responses.size());
            verify(ticketRepository).findByAssignmentIdIn(assignmentIds);
        }

        @Test
        @DisplayName("Should get tickets by list of reporter IDs")
        void shouldGetTicketsByReporterIds() {
            // Arrange
            List<String> reporterIds = List.of("reporter-1", "reporter-2");
            
            Ticket ticket1 = new Ticket();
            ticket1.setId(UUID.randomUUID());
            ticket1.setReporterId("reporter-1");

            Ticket ticket2 = new Ticket();
            ticket2.setId(UUID.randomUUID());
            ticket2.setReporterId("reporter-2");

            when(ticketRepository.findByReporterIdIn(reporterIds)).thenReturn(List.of(ticket1, ticket2));

            // Act
            List<TicketResponse> responses = ticketService.getTicketsByReporterIds(reporterIds);

            // Assert
            assertNotNull(responses);
            assertEquals(2, responses.size());
            verify(ticketRepository).findByReporterIdIn(reporterIds);
        }
    }

    @Nested
    @DisplayName("Filter Tests")
    class FilterTests {

        @Test
        @DisplayName("Should get tickets by status")
        void shouldGetTicketsByStatus() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Ticket> ticketPage = new PageImpl<>(List.of(ticket), pageable, 1);

            when(ticketRepository.findByStatus(TicketStatus.OPEN, pageable)).thenReturn(ticketPage);

            // Act
            Page<TicketResponse> response = ticketService.getTicketsByStatus(TicketStatus.OPEN, pageable);

            // Assert
            assertNotNull(response);
            assertEquals(1, response.getTotalElements());
            verify(ticketRepository).findByStatus(TicketStatus.OPEN, pageable);
        }

        @Test
        @DisplayName("Should get open tickets")
        void shouldGetOpenTickets() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Ticket> ticketPage = new PageImpl<>(List.of(ticket), pageable, 1);

            when(ticketRepository.findOpenTickets(pageable)).thenReturn(ticketPage);

            // Act
            Page<TicketResponse> response = ticketService.getOpenTickets(pageable);

            // Assert
            assertNotNull(response);
            assertEquals(1, response.getTotalElements());
            verify(ticketRepository).findOpenTickets(pageable);
        }
    }
}
