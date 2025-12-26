package com.ds.session.session_service.infrastructure.osrm;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ds.session.session_service.application.client.zoneclient.ZoneServiceClient;
import com.ds.session.session_service.application.client.zoneclient.request.TableMatrixRequest;
import com.ds.session.session_service.application.client.zoneclient.response.BaseResponse;
import com.ds.session.session_service.application.client.zoneclient.response.TableMatrixResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to call OSRM Table API via zone-service
 * Zone-service handles all routing logic, this service only provides matrix data
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OSRMService {

    private final ZoneServiceClient zoneServiceClient;

    /**
     * Get distance/duration matrix from OSRM Table API via zone-service
     * 
     * @param coordinates List of [lat, lon] coordinates
     * @param vehicle Vehicle type: "car" or "motorbike" (default: "motorbike")
     * @param mode OSRM mode: "v2-full", "v2-rating-only", etc. (default: "v2-full")
     * @return OSRMMatrixResponse containing durations and distances
     */
    public OSRMMatrixResponse getMatrix(List<double[]> coordinates, String vehicle, String mode) {
        if (coordinates == null || coordinates.isEmpty()) {
            throw new IllegalArgumentException("Coordinates list cannot be empty");
        }

        // Default values
        if (vehicle == null || vehicle.isBlank()) {
            vehicle = "motorbike";
        }
        if (mode == null || mode.isBlank()) {
            mode = "v2-full";
        }

        // Convert coordinates to request DTO
        List<TableMatrixRequest.Coordinate> coordList = coordinates.stream()
                .map(coord -> {
                    if (coord.length < 2) {
                        throw new IllegalArgumentException("Each coordinate must have at least 2 elements [lat, lon]");
                    }
                    return TableMatrixRequest.Coordinate.builder()
                            .lat(coord[0])
                            .lon(coord[1])
                            .build();
                })
                .collect(Collectors.toList());

        TableMatrixRequest request = TableMatrixRequest.builder()
                .coordinates(coordList)
                .vehicle(vehicle)
                .mode(mode)
                .build();

        log.debug("[session-service] [OSRMService.getMatrix] Calling zone-service table-matrix API: {} coordinates, vehicle={}, mode={}", 
                coordinates.size(), vehicle, mode);

        try {
            BaseResponse<TableMatrixResponse> response = zoneServiceClient.getTableMatrix(request);
            
            if (response == null || response.getResult() == null) {
                throw new RuntimeException("Zone-service returned null response");
            }

            TableMatrixResponse matrixResponse = response.getResult();
            
            if (!"Ok".equals(matrixResponse.getCode())) {
                log.warn("[session-service] [OSRMService.getMatrix] Zone-service returned code: {}", matrixResponse.getCode());
            }

            log.debug("[session-service] [OSRMService.getMatrix] Successfully retrieved matrix: {}x{}", 
                    matrixResponse.getDurations() != null ? matrixResponse.getDurations().size() : 0,
                    matrixResponse.getDurations() != null && !matrixResponse.getDurations().isEmpty() ? matrixResponse.getDurations().get(0).size() : 0);

            // Convert to OSRMMatrixResponse
            return OSRMMatrixResponse.builder()
                    .code(matrixResponse.getCode())
                    .durations(matrixResponse.getDurations())
                    .distances(matrixResponse.getDistances())
                    .sources(convertWaypoints(matrixResponse.getSources()))
                    .destinations(convertWaypoints(matrixResponse.getDestinations()))
                    .build();
        } catch (Exception e) {
            log.error("[session-service] [OSRMService.getMatrix] Error calling zone-service table-matrix API", e);
            throw new RuntimeException("Failed to call zone-service table-matrix API: " + e.getMessage(), e);
        }
    }

    /**
     * Convert TableMatrixResponse.Waypoint list to OSRMMatrixResponse.Waypoint list
     */
    private List<OSRMMatrixResponse.Waypoint> convertWaypoints(List<TableMatrixResponse.Waypoint> waypoints) {
        if (waypoints == null) {
            return null;
        }
        return waypoints.stream()
                .map(wp -> OSRMMatrixResponse.Waypoint.builder()
                        .hint(wp.getHint())
                        .distance(wp.getDistance())
                        .name(wp.getName())
                        .location(wp.getLocation())
                        .build())
                .collect(Collectors.toList());
    }
}
