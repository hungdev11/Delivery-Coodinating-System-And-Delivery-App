package com.ds.parcel_service.application.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ds.parcel_service.common.entities.dto.common.BaseResponse;
import com.ds.parcel_service.common.entities.dto.request.ParcelCreateRequest;
import com.ds.parcel_service.common.entities.dto.request.ParcelUpdateRequest;
import com.ds.parcel_service.common.entities.dto.response.ParcelResponse;
import com.ds.parcel_service.common.enums.DeliveryType;
import com.ds.parcel_service.common.enums.ParcelStatus;
import com.ds.parcel_service.common.interfaces.IParcelService;

/**
 * Unit tests for ParcelController
 * Tests controller methods directly with mocked services (no server running)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ParcelController Unit Tests")
class ParcelControllerTest {

    @Mock
    private IParcelService parcelService;

    @InjectMocks
    private ParcelController parcelController;

    private UUID parcelId;
    private ParcelResponse mockResponse;
    private ParcelCreateRequest createRequest;
    private ParcelUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        parcelId = UUID.randomUUID();
        String senderAddressId = UUID.randomUUID().toString();
        String receiverAddressId = UUID.randomUUID().toString();

        createRequest = ParcelCreateRequest.builder()
                .code("TEST-001")
                .senderId(UUID.randomUUID().toString())
                .receiverId(UUID.randomUUID().toString())
                .senderAddressId(senderAddressId)
                .receiverAddressId(receiverAddressId)
                .deliveryType("NORMAL")
                .weight(1.5)
                .value(new BigDecimal("100000"))
                .windowStart(LocalTime.of(8, 0))
                .windowEnd(LocalTime.of(18, 0))
                .build();

        updateRequest = ParcelUpdateRequest.builder()
                .weight(2.0)
                .value(new BigDecimal("200000"))
                .build();

        mockResponse = ParcelResponse.builder()
                .id(parcelId.toString())
                .code("TEST-001")
                .senderId(createRequest.getSenderId())
                .receiverId(createRequest.getReceiverId())
                .senderAddressId(senderAddressId)
                .receiverAddressId(receiverAddressId)
                .deliveryType(DeliveryType.NORMAL)
                .status(ParcelStatus.IN_WAREHOUSE)
                .weight(1.5)
                .value(new BigDecimal("100000"))
                .priority(3)
                .build();
    }

    @Nested
    @DisplayName("createParcel Method Tests")
    class CreateParcelTests {

        @Test
        @DisplayName("Should create parcel and return CREATED status with correct response")
        void shouldCreateParcelSuccessfully() {
            // Arrange
            when(parcelService.createParcel(any(ParcelCreateRequest.class))).thenReturn(mockResponse);

            // Act
            ResponseEntity<BaseResponse<ParcelResponse>> response = parcelController.createParcel(createRequest);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getResult());
            
            ParcelResponse result = response.getBody().getResult();
            assertEquals(parcelId.toString(), result.getId());
            assertEquals("TEST-001", result.getCode());
            assertEquals(createRequest.getSenderAddressId(), result.getSenderAddressId());
            assertEquals(createRequest.getReceiverAddressId(), result.getReceiverAddressId());
            assertEquals(3, result.getPriority());

            verify(parcelService, times(1)).createParcel(any(ParcelCreateRequest.class));
        }

        @Test
        @DisplayName("Should propagate service exceptions")
        void shouldPropagateServiceExceptions() {
            // Arrange
            when(parcelService.createParcel(any(ParcelCreateRequest.class)))
                    .thenThrow(new IllegalArgumentException("Invalid request"));

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                parcelController.createParcel(createRequest);
            });

            verify(parcelService, times(1)).createParcel(any(ParcelCreateRequest.class));
        }
    }

    @Nested
    @DisplayName("getParcelById Method Tests")
    class GetParcelByIdTests {

        @Test
        @DisplayName("Should get parcel by ID and return OK status")
        void shouldGetParcelByIdSuccessfully() {
            // Arrange
            when(parcelService.getParcelById(parcelId)).thenReturn(mockResponse);

            // Act
            ResponseEntity<BaseResponse<ParcelResponse>> response = parcelController.getParcelById(parcelId);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getResult());
            
            ParcelResponse result = response.getBody().getResult();
            assertEquals(parcelId.toString(), result.getId());
            assertEquals("TEST-001", result.getCode());
            assertNotNull(result.getSenderAddressId());
            assertNotNull(result.getReceiverAddressId());

            verify(parcelService, times(1)).getParcelById(parcelId);
        }

        @Test
        @DisplayName("Should propagate ResourceNotFound exception")
        void shouldPropagateResourceNotFound() {
            // Arrange
            when(parcelService.getParcelById(parcelId))
                    .thenThrow(new com.ds.parcel_service.common.exceptions.ResourceNotFound("Parcel not found"));

            // Act & Assert
            assertThrows(com.ds.parcel_service.common.exceptions.ResourceNotFound.class, () -> {
                parcelController.getParcelById(parcelId);
            });

            verify(parcelService, times(1)).getParcelById(parcelId);
        }
    }

    @Nested
    @DisplayName("updateParcel Method Tests")
    class UpdateParcelTests {

        @Test
        @DisplayName("Should update parcel and return OK status")
        void shouldUpdateParcelSuccessfully() {
            // Arrange
            ParcelResponse updatedResponse = ParcelResponse.builder()
                    .id(parcelId.toString())
                    .code("TEST-001")
                    .weight(2.0)
                    .value(new BigDecimal("200000"))
                    .build();

            when(parcelService.updateParcel(eq(parcelId), any(ParcelUpdateRequest.class)))
                    .thenReturn(updatedResponse);

            // Act
            ResponseEntity<BaseResponse<ParcelResponse>> response = 
                    parcelController.updateParcel(parcelId, updateRequest);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getResult());
            
            ParcelResponse result = response.getBody().getResult();
            assertEquals(2.0, result.getWeight());
            assertEquals(new BigDecimal("200000"), result.getValue());

            verify(parcelService, times(1)).updateParcel(eq(parcelId), any(ParcelUpdateRequest.class));
        }
    }

    @Nested
    @DisplayName("getParcelByCode Method Tests")
    class GetParcelByCodeTests {

        @Test
        @DisplayName("Should get parcel by code and return OK status")
        void shouldGetParcelByCodeSuccessfully() {
            // Arrange
            String code = "TEST-001";
            when(parcelService.getParcelByCode(code)).thenReturn(mockResponse);

            // Act
            ResponseEntity<BaseResponse<ParcelResponse>> response = parcelController.getParcelByCode(code);

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().getResult());
            
            ParcelResponse result = response.getBody().getResult();
            assertEquals(code, result.getCode());

            verify(parcelService, times(1)).getParcelByCode(code);
        }
    }
}
