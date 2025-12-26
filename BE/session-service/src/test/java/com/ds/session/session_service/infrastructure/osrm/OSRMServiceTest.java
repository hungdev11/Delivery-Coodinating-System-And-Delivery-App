package com.ds.session.session_service.infrastructure.osrm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ds.session.session_service.application.client.zoneclient.ZoneServiceClient;
import com.ds.session.session_service.application.client.zoneclient.request.TableMatrixRequest;
import com.ds.session.session_service.application.client.zoneclient.response.BaseResponse;
import com.ds.session.session_service.application.client.zoneclient.response.TableMatrixResponse;

/**
 * Unit tests for OSRMService
 * Tests OSRM table matrix API calls via zone-service
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OSRMService Tests")
class OSRMServiceTest {

    @Mock
    private ZoneServiceClient zoneServiceClient;

    @InjectMocks
    private OSRMService osrmService;

    private List<double[]> coordinates;
    private TableMatrixResponse mockMatrixResponse;

    @BeforeEach
    void setUp() {
        coordinates = Arrays.asList(
            new double[]{10.8505, 106.7718}, // Location 1
            new double[]{10.8550, 106.7800}, // Location 2
            new double[]{10.8623, 106.8032}  // Location 3
        );

        // Create mock matrix response
        mockMatrixResponse = TableMatrixResponse.builder()
            .code("Ok")
            .durations(Arrays.asList(
                Arrays.asList(0.0, 120.0, 300.0),  // From location 1
                Arrays.asList(130.0, 0.0, 200.0),  // From location 2
                Arrays.asList(310.0, 210.0, 0.0)   // From location 3
            ))
            .distances(Arrays.asList(
                Arrays.asList(0.0, 1500.0, 5000.0),  // From location 1
                Arrays.asList(1600.0, 0.0, 3500.0),  // From location 2
                Arrays.asList(5100.0, 3600.0, 0.0)   // From location 3
            ))
            .build();
    }

    @Nested
    @DisplayName("getMatrix Tests")
    class GetMatrixTests {

        @Test
        @DisplayName("Should get matrix successfully with motorbike vehicle")
        void shouldGetMatrixSuccessfullyWithMotorbike() {
            // Arrange
            BaseResponse<TableMatrixResponse> response = BaseResponse.<TableMatrixResponse>builder()
                .success(true)
                .result(mockMatrixResponse)
                .build();

            when(zoneServiceClient.getTableMatrix(any(TableMatrixRequest.class))).thenReturn(response);

            // Act
            OSRMMatrixResponse result = osrmService.getMatrix(coordinates, "motorbike", "v2-full");

            // Assert
            assertNotNull(result);
            assertEquals("Ok", result.getCode());
            assertNotNull(result.getDurations());
            assertEquals(3, result.getDurations().size());
            assertEquals(3, result.getDurations().get(0).size());
            assertNotNull(result.getDistances());
            assertEquals(3, result.getDistances().size());

            verify(zoneServiceClient, times(1)).getTableMatrix(any(TableMatrixRequest.class));
        }

        @Test
        @DisplayName("Should get matrix successfully with car vehicle")
        void shouldGetMatrixSuccessfullyWithCar() {
            // Arrange
            BaseResponse<TableMatrixResponse> response = BaseResponse.<TableMatrixResponse>builder()
                .success(true)
                .result(mockMatrixResponse)
                .build();

            when(zoneServiceClient.getTableMatrix(any(TableMatrixRequest.class))).thenReturn(response);

            // Act
            OSRMMatrixResponse result = osrmService.getMatrix(coordinates, "car", "v2-car-full");

            // Assert
            assertNotNull(result);
            assertEquals("Ok", result.getCode());
            verify(zoneServiceClient, times(1)).getTableMatrix(argThat(req -> 
                "car".equals(req.getVehicle()) && "v2-car-full".equals(req.getMode())
            ));
        }

        @Test
        @DisplayName("Should use default values when vehicle and mode are null")
        void shouldUseDefaultValuesWhenVehicleAndModeAreNull() {
            // Arrange
            BaseResponse<TableMatrixResponse> response = BaseResponse.<TableMatrixResponse>builder()
                .success(true)
                .result(mockMatrixResponse)
                .build();

            when(zoneServiceClient.getTableMatrix(any(TableMatrixRequest.class))).thenReturn(response);

            // Act
            OSRMMatrixResponse result = osrmService.getMatrix(coordinates, null, null);

            // Assert
            assertNotNull(result);
            verify(zoneServiceClient, times(1)).getTableMatrix(argThat(req -> 
                "motorbike".equals(req.getVehicle()) && "v2-full".equals(req.getMode())
            ));
        }

        @Test
        @DisplayName("Should throw exception when coordinates list is empty")
        void shouldThrowExceptionWhenCoordinatesListIsEmpty() {
            // Arrange
            List<double[]> emptyCoordinates = new ArrayList<>();

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                osrmService.getMatrix(emptyCoordinates, "motorbike", "v2-full");
            });

            assertEquals("Coordinates list cannot be empty", exception.getMessage());
            verify(zoneServiceClient, never()).getTableMatrix(any());
        }

        @Test
        @DisplayName("Should throw exception when coordinates list is null")
        void shouldThrowExceptionWhenCoordinatesListIsNull() {
            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                osrmService.getMatrix(null, "motorbike", "v2-full");
            });

            assertEquals("Coordinates list cannot be empty", exception.getMessage());
            verify(zoneServiceClient, never()).getTableMatrix(any());
        }

        @Test
        @DisplayName("Should throw exception when coordinate has less than 2 elements")
        void shouldThrowExceptionWhenCoordinateHasLessThan2Elements() {
            // Arrange
            List<double[]> invalidCoordinates = Arrays.asList(
                new double[]{10.8505} // Only lat, missing lon
            );

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                osrmService.getMatrix(invalidCoordinates, "motorbike", "v2-full");
            });

            assertEquals("Each coordinate must have at least 2 elements [lat, lon]", exception.getMessage());
            verify(zoneServiceClient, never()).getTableMatrix(any());
        }

        @Test
        @DisplayName("Should throw exception when zone-service returns null response")
        void shouldThrowExceptionWhenZoneServiceReturnsNullResponse() {
            // Arrange
            when(zoneServiceClient.getTableMatrix(any(TableMatrixRequest.class))).thenReturn(null);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                osrmService.getMatrix(coordinates, "motorbike", "v2-full");
            });

            assertTrue(exception.getMessage().contains("Zone-service returned null response"));
            verify(zoneServiceClient, times(1)).getTableMatrix(any(TableMatrixRequest.class));
        }

        @Test
        @DisplayName("Should throw exception when zone-service returns null result")
        void shouldThrowExceptionWhenZoneServiceReturnsNullResult() {
            // Arrange
            BaseResponse<TableMatrixResponse> response = BaseResponse.<TableMatrixResponse>builder()
                .success(true)
                .result(null)
                .build();

            when(zoneServiceClient.getTableMatrix(any(TableMatrixRequest.class))).thenReturn(response);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                osrmService.getMatrix(coordinates, "motorbike", "v2-full");
            });

            assertTrue(exception.getMessage().contains("Zone-service returned null response"));
            verify(zoneServiceClient, times(1)).getTableMatrix(any(TableMatrixRequest.class));
        }

        @Test
        @DisplayName("Should handle zone-service exception")
        void shouldHandleZoneServiceException() {
            // Arrange
            when(zoneServiceClient.getTableMatrix(any(TableMatrixRequest.class)))
                .thenThrow(new RuntimeException("Zone-service unavailable"));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                osrmService.getMatrix(coordinates, "motorbike", "v2-full");
            });

            assertTrue(exception.getMessage().contains("Failed to call zone-service table-matrix API"));
            verify(zoneServiceClient, times(1)).getTableMatrix(any(TableMatrixRequest.class));
        }
    }
}
