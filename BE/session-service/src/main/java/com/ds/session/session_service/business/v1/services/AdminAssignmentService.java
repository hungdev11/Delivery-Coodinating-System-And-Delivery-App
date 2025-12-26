package com.ds.session.session_service.business.v1.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ds.session.session_service.app_context.models.DeliveryAssignment;
import com.ds.session.session_service.app_context.models.DeliveryAssignmentParcel;
import com.ds.session.session_service.app_context.repositories.DeliveryAssignmentParcelRepository;
import com.ds.session.session_service.app_context.repositories.DeliveryAssignmentRepository;
import com.ds.session.session_service.application.client.parcelclient.ParcelServiceClient;
import com.ds.session.session_service.application.client.parcelclient.response.ParcelResponse;
import com.ds.session.session_service.application.client.zoneclient.ZoneServiceClient;
import com.ds.session.session_service.application.client.zoneclient.request.VRPAssignmentRequest;
import com.ds.session.session_service.application.client.zoneclient.response.BaseResponse;
import com.ds.session.session_service.application.client.zoneclient.response.VRPAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.request.AutoAssignmentRequest;
import com.ds.session.session_service.common.entities.dto.request.ManualAssignmentRequest;
import com.ds.session.session_service.common.entities.dto.response.AutoAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.ManualAssignmentResponse;
import com.ds.session.session_service.common.enums.AssignmentStatus;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for admin to manually create delivery assignments
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminAssignmentService {
    
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryAssignmentParcelRepository assignmentParcelRepository;
    private final ParcelServiceClient parcelServiceClient;
    private final ZoneServiceClient zoneServiceClient;
    
    /**
     * Create a manual assignment for a shipper with specified parcels
     * 
     * Validations:
     * - All parcels must have the same delivery address
     * - Parcels must be in shipper's working zones (if zoneId provided)
     * - Parcels must not already be assigned
     * 
     * @param request Manual assignment request
     * @return Created assignment response
     */
    public ManualAssignmentResponse createManualAssignment(ManualAssignmentRequest request) {
        log.info("[AdminAssignmentService] Creating manual assignment for shipper {} with {} parcels", 
            request.getShipperId(), request.getParcelIds().size());
        
        // 1. Fetch all parcels
        List<ParcelResponse> parcels = new ArrayList<>();
        for (String parcelId : request.getParcelIds()) {
            ParcelResponse parcel = parcelServiceClient.fetchParcelResponse(parcelId);
            if (parcel == null) {
                throw new IllegalArgumentException("Parcel not found: " + parcelId);
            }
            parcels.add(parcel);
        }
        
        if (parcels.isEmpty()) {
            throw new IllegalArgumentException("No valid parcels found");
        }
        
        // 2. Validate all parcels have the same delivery address
        String firstDeliveryAddressId = parcels.get(0).getReceiverAddressId();
        if (firstDeliveryAddressId == null || firstDeliveryAddressId.isEmpty()) {
            throw new IllegalArgumentException("Parcel " + parcels.get(0).getId() + " does not have a delivery address");
        }
        
        for (ParcelResponse parcel : parcels) {
            if (parcel.getReceiverAddressId() == null || parcel.getReceiverAddressId().isEmpty()) {
                throw new IllegalArgumentException("Parcel " + parcel.getId() + " does not have a delivery address");
            }
            if (!firstDeliveryAddressId.equals(parcel.getReceiverAddressId())) {
                throw new IllegalArgumentException(
                    "All parcels must have the same delivery address. " +
                    "Parcel " + parcel.getId() + " has address " + parcel.getReceiverAddressId() + 
                    " but expected " + firstDeliveryAddressId
                );
            }
        }
        
        // 3. Filter parcels by zone if zoneId provided
        List<ParcelResponse> filteredParcels = parcels;
        if (request.getZoneId() != null && !request.getZoneId().isEmpty()) {
            log.debug("[AdminAssignmentService] Filtering parcels by zone: {}", request.getZoneId());
            filteredParcels = filterParcelsByZone(parcels, request.getZoneId());
            if (filteredParcels.isEmpty()) {
                throw new IllegalArgumentException(
                    "No parcels found in zone " + request.getZoneId() + 
                    ". Please check zone ID or parcel locations."
                );
            }
            log.debug("[AdminAssignmentService] Filtered to {} parcels in zone {}", 
                filteredParcels.size(), request.getZoneId());
        }
        
        // 4. Check if parcels are already assigned
        Set<String> parcelIds = filteredParcels.stream()
            .map(ParcelResponse::getId)
            .collect(Collectors.toSet());
        
        for (String parcelId : parcelIds) {
            boolean exists = assignmentParcelRepository.existsByParcelId(parcelId);
            if (exists) {
                throw new IllegalArgumentException("Parcel " + parcelId + " is already assigned to another assignment");
            }
        }
        
        // 5. Create assignment with status PENDING
        DeliveryAssignment assignment = DeliveryAssignment.builder()
            .shipperId(request.getShipperId())
            .deliveryAddressId(firstDeliveryAddressId)
            .status(AssignmentStatus.PENDING)
            .assignedAt(LocalDateTime.now())
            .parcels(new ArrayList<>())
            .build();
        
        assignment = assignmentRepository.save(assignment);
        log.info("[AdminAssignmentService] Created assignment {} with status PENDING", assignment.getId());
        
        // 6. Create DeliveryAssignmentParcel records
        for (ParcelResponse parcel : filteredParcels) {
            DeliveryAssignmentParcel assignmentParcel = DeliveryAssignmentParcel.builder()
                .assignment(assignment)
                .parcelId(parcel.getId())
                .build();
            assignment.addParcel(assignmentParcel);
        }
        
        assignment = assignmentRepository.save(assignment);
        log.info("[AdminAssignmentService] Added {} parcels to assignment {}", 
            filteredParcels.size(), assignment.getId());
        
        // 7. Build response
        return ManualAssignmentResponse.builder()
            .assignmentId(assignment.getId())
            .shipperId(assignment.getShipperId())
            .deliveryAddressId(assignment.getDeliveryAddressId())
            .parcelIds(parcelIds.stream().collect(Collectors.toList()))
            .status(assignment.getStatus())
            .assignedAt(assignment.getAssignedAt())
            .zoneId(request.getZoneId())
            .build();
    }
    
    /**
     * Filter parcels by zone ID
     * Uses zone-service to check if parcel location is in the specified zone
     */
    private List<ParcelResponse> filterParcelsByZone(List<ParcelResponse> parcels, String zoneId) {
        List<ParcelResponse> filtered = new ArrayList<>();
        
        for (ParcelResponse parcel : parcels) {
            if (parcel.getLat() == null || parcel.getLon() == null) {
                log.warn("[AdminAssignmentService] Parcel {} does not have location, skipping zone filter", parcel.getId());
                continue;
            }
            
            try {
                // Call zone-service to check if parcel is in zone
                // Note: This requires zone-service to have an endpoint to check point-in-zone
                // For now, we'll skip zone validation if zone-service doesn't support it
                // TODO: Implement zone validation when zone-service API is available
                log.debug("[AdminAssignmentService] Checking if parcel {} (lat: {}, lon: {}) is in zone {}", 
                    parcel.getId(), parcel.getLat(), parcel.getLon(), zoneId);
                
                // For now, include all parcels with valid coordinates
                // Zone validation can be added later when zone-service API is ready
                filtered.add(parcel);
            } catch (Exception e) {
                log.warn("[AdminAssignmentService] Error checking zone for parcel {}: {}", parcel.getId(), e.getMessage());
                // Skip this parcel if zone check fails
            }
        }
        
        return filtered;
    }
    
    /**
     * Create auto assignments using VRP solver from zone-service
     * 
     * Algorithm:
     * 1. Fetch shippers and parcels
     * 2. Convert to VRP DTOs
     * 3. Call zone-service VRP API
     * 4. Create assignments and DeliveryAssignmentParcel records from results
     * 5. Group parcels by delivery address (all parcels with same deliveryAddressId in same assignment)
     * 
     * @param request Auto assignment request
     * @return Auto assignment response with created assignments
     */
    public AutoAssignmentResponse createAutoAssignment(AutoAssignmentRequest request) {
        log.info("[AdminAssignmentService] Creating auto assignment with {} shippers, {} parcels", 
            request.getShipperIds() != null ? request.getShipperIds().size() : "all",
            request.getParcelIds() != null ? request.getParcelIds().size() : "all");
        
        // 1. Fetch parcels
        List<ParcelResponse> parcels = fetchParcels(request.getParcelIds());
        if (parcels.isEmpty()) {
            throw new IllegalArgumentException("No valid parcels found for assignment");
        }
        
        // Filter out already assigned parcels
        parcels = parcels.stream()
            .filter(p -> !assignmentParcelRepository.existsByParcelId(p.getId()))
            .collect(Collectors.toList());
        
        if (parcels.isEmpty()) {
            throw new IllegalArgumentException("All parcels are already assigned");
        }
        
        // 2. Fetch shippers (for now, use default values - TODO: fetch from user-service)
        List<VRPAssignmentRequest.VRPShipperDto> shippers = fetchShippers(request.getShipperIds());
        if (shippers.isEmpty()) {
            throw new IllegalArgumentException("No valid shippers found for assignment");
        }
        
        // 3. Convert parcels to VRP orders
        List<VRPAssignmentRequest.VRPOrderDto> orders = parcels.stream()
            .map(this::convertToVRPOrder)
            .filter(o -> o != null)
            .collect(Collectors.toList());
        
        if (orders.isEmpty()) {
            throw new IllegalArgumentException("No valid orders with location information");
        }
        
        // 4. Call zone-service VRP API
        VRPAssignmentRequest vrpRequest = VRPAssignmentRequest.builder()
            .shippers(shippers)
            .orders(orders)
            .vehicle(request.getVehicle() != null ? request.getVehicle() : "motorbike")
            .mode(request.getMode() != null ? request.getMode() : "v2-full")
            .build();
        
        log.info("[AdminAssignmentService] Calling zone-service VRP API with {} shippers, {} orders", 
            shippers.size(), orders.size());
        
        BaseResponse<VRPAssignmentResponse> vrpResponse = zoneServiceClient.solveVRPAssignment(vrpRequest);
        
        if (vrpResponse == null || !Boolean.TRUE.equals(vrpResponse.getSuccess()) || vrpResponse.getResult() == null) {
            throw new RuntimeException("Failed to solve VRP assignment: " + 
                (vrpResponse != null ? vrpResponse.getMessage() : "null response"));
        }
        
        VRPAssignmentResponse vrpResult = vrpResponse.getResult();
        log.info("[AdminAssignmentService] VRP solved: {} assignments, {} unassigned orders", 
            vrpResult.getAssignments().size(), 
            vrpResult.getUnassignedOrders() != null ? vrpResult.getUnassignedOrders().size() : 0);
        
        // 5. Create assignments from VRP results
        // Group tasks by delivery address (all parcels with same deliveryAddressId in same assignment)
        Map<String, List<AutoAssignmentResponse.AssignmentInfo>> assignmentsByShipper = new HashMap<>();
        Map<String, Map<String, List<String>>> shipperAddressParcels = new HashMap<>(); // shipperId -> deliveryAddressId -> parcelIds
        
        for (Map.Entry<String, List<VRPAssignmentResponse.VRPTaskDto>> entry : vrpResult.getAssignments().entrySet()) {
            String shipperId = entry.getKey();
            List<VRPAssignmentResponse.VRPTaskDto> tasks = entry.getValue();
            
            // Group tasks by delivery address
            Map<String, List<String>> addressParcels = new HashMap<>();
            for (VRPAssignmentResponse.VRPTaskDto task : tasks) {
                ParcelResponse parcel = parcels.stream()
                    .filter(p -> p.getId().equals(task.getOrderId()))
                    .findFirst()
                    .orElse(null);
                
                if (parcel == null || parcel.getReceiverAddressId() == null) {
                    log.warn("[AdminAssignmentService] Parcel {} not found or missing delivery address", task.getOrderId());
                    continue;
                }
                
                String deliveryAddressId = parcel.getReceiverAddressId();
                addressParcels.computeIfAbsent(deliveryAddressId, k -> new ArrayList<>()).add(task.getOrderId());
            }
            
            shipperAddressParcels.put(shipperId, addressParcels);
        }
        
        // 6. Create DeliveryAssignment records grouped by delivery address
        List<AutoAssignmentResponse.AssignmentInfo> allAssignments = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, List<String>>> shipperEntry : shipperAddressParcels.entrySet()) {
            String shipperId = shipperEntry.getKey();
            Map<String, List<String>> addressParcels = shipperEntry.getValue();
            
            List<AutoAssignmentResponse.AssignmentInfo> shipperAssignments = new ArrayList<>();
            
            for (Map.Entry<String, List<String>> addressEntry : addressParcels.entrySet()) {
                String deliveryAddressId = addressEntry.getKey();
                List<String> parcelIds = addressEntry.getValue();
                
                // Create assignment
                DeliveryAssignment assignment = DeliveryAssignment.builder()
                    .shipperId(shipperId)
                    .deliveryAddressId(deliveryAddressId)
                    .status(AssignmentStatus.PENDING)
                    .assignedAt(LocalDateTime.now())
                    .parcels(new ArrayList<>())
                    .build();
                
                assignment = assignmentRepository.save(assignment);
                log.debug("[AdminAssignmentService] Created assignment {} for shipper {} with {} parcels", 
                    assignment.getId(), shipperId, parcelIds.size());
                
                // Create DeliveryAssignmentParcel records
                for (String parcelId : parcelIds) {
                    DeliveryAssignmentParcel assignmentParcel = DeliveryAssignmentParcel.builder()
                        .assignment(assignment)
                        .parcelId(parcelId)
                        .build();
                    assignment.addParcel(assignmentParcel);
                }
                
                assignment = assignmentRepository.save(assignment);
                
                // Build assignment info
                AutoAssignmentResponse.AssignmentInfo assignmentInfo = AutoAssignmentResponse.AssignmentInfo.builder()
                    .assignmentId(assignment.getId())
                    .deliveryAddressId(deliveryAddressId)
                    .parcelIds(parcelIds)
                    .status(assignment.getStatus())
                    .build();
                
                shipperAssignments.add(assignmentInfo);
                allAssignments.add(assignmentInfo);
            }
            
            assignmentsByShipper.put(shipperId, shipperAssignments);
        }
        
        // 7. Calculate statistics
        int totalShippers = assignmentsByShipper.size();
        int totalParcels = parcels.size();
        int assignedParcels = allAssignments.stream()
            .mapToInt(a -> a.getParcelIds().size())
            .sum();
        double avgParcelsPerShipper = totalShippers > 0 ? (double) assignedParcels / totalShippers : 0.0;
        
        // Calculate workload variance
        List<Integer> parcelsPerShipper = assignmentsByShipper.values().stream()
            .mapToInt(List::size)
            .boxed()
            .collect(Collectors.toList());
        double variance = calculateVariance(parcelsPerShipper);
        
        // 8. Build response
        return AutoAssignmentResponse.builder()
            .assignments(assignmentsByShipper)
            .unassignedParcels(vrpResult.getUnassignedOrders() != null ? vrpResult.getUnassignedOrders() : new ArrayList<>())
            .statistics(AutoAssignmentResponse.Statistics.builder()
                .totalShippers(totalShippers)
                .totalParcels(totalParcels)
                .assignedParcels(assignedParcels)
                .averageParcelsPerShipper(avgParcelsPerShipper)
                .workloadVariance(variance)
                .build())
            .build();
    }
    
    /**
     * Fetch parcels from parcel-service
     */
    private List<ParcelResponse> fetchParcels(List<String> parcelIds) {
        if (parcelIds == null || parcelIds.isEmpty()) {
            // TODO: Fetch all unassigned parcels from parcel-service
            // For now, return empty list
            log.warn("[AdminAssignmentService] Fetching all parcels not yet implemented");
            return new ArrayList<>();
        }
        
        List<ParcelResponse> parcels = new ArrayList<>();
        for (String parcelId : parcelIds) {
            ParcelResponse parcel = parcelServiceClient.fetchParcelResponse(parcelId);
            if (parcel != null) {
                parcels.add(parcel);
            } else {
                log.warn("[AdminAssignmentService] Parcel not found: {}", parcelId);
            }
        }
        return parcels;
    }
    
    /**
     * Fetch shippers (for now, use default values - TODO: fetch from user-service with working zones)
     */
    private List<VRPAssignmentRequest.VRPShipperDto> fetchShippers(List<String> shipperIds) {
        // TODO: Fetch shippers from user-service with working zones, shift times, etc.
        // For now, use default values
        List<VRPAssignmentRequest.VRPShipperDto> shippers = new ArrayList<>();
        
        if (shipperIds == null || shipperIds.isEmpty()) {
            // TODO: Fetch all available shippers from user-service
            log.warn("[AdminAssignmentService] Fetching all shippers not yet implemented");
            return shippers;
        }
        
        for (String shipperId : shipperIds) {
            // Default values - should be fetched from user-service
            VRPAssignmentRequest.VRPShipperDto shipper = VRPAssignmentRequest.VRPShipperDto.builder()
                .shipperId(shipperId)
                .lat(10.8505) // Default location (Ho Chi Minh City)
                .lon(106.7718)
                .shiftStart("08:00:00") // Default shift start
                .maxSessionTime(4.5) // Default max session time (hours)
                .capacity(10) // Default capacity
                .zoneIds(new ArrayList<>()) // TODO: Fetch from user-service
                .vehicle("motorbike") // Default vehicle type
                .build();
            shippers.add(shipper);
        }
        
        return shippers;
    }
    
    /**
     * Convert ParcelResponse to VRPOrderDto
     */
    private VRPAssignmentRequest.VRPOrderDto convertToVRPOrder(ParcelResponse parcel) {
        if (parcel.getLat() == null || parcel.getLon() == null) {
            log.warn("[AdminAssignmentService] Parcel {} does not have location information", parcel.getId());
            return null;
        }
        
        // Get priority from delivery type (default to 3 if not available)
        int priority = 3; // Default priority
        if (parcel.getDeliveryType() != null) {
            // Map delivery type to priority (0 = urgent, higher = less urgent)
            switch (parcel.getDeliveryType().toUpperCase()) {
                case "URGENT":
                    priority = 0;
                    break;
                case "EXPRESS":
                    priority = 1;
                    break;
                case "FAST":
                    priority = 2;
                    break;
                case "NORMAL":
                    priority = 3;
                    break;
                case "ECONOMY":
                    priority = 4;
                    break;
                default:
                    priority = 3;
            }
        }
        
        return VRPAssignmentRequest.VRPOrderDto.builder()
            .orderId(parcel.getId())
            .lat(parcel.getLat().doubleValue())
            .lon(parcel.getLon().doubleValue())
            .serviceTime(300) // Default 5 minutes service time
            .priority(priority)
            .zoneId(null) // TODO: Get from parcel location
            .deliveryAddressId(parcel.getReceiverAddressId())
            .build();
    }
    
    /**
     * Calculate variance of a list of integers
     */
    private double calculateVariance(List<Integer> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0.0);
        
        return variance;
    }
}
