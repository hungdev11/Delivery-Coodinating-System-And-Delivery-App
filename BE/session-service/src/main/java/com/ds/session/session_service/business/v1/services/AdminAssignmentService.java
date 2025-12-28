package com.ds.session.session_service.business.v1.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ds.session.session_service.app_context.models.DeliveryAssignment;
import com.ds.session.session_service.app_context.models.DeliveryAssignmentParcel;
import com.ds.session.session_service.app_context.models.DeliverySession;
import com.ds.session.session_service.app_context.repositories.DeliveryAssignmentParcelRepository;
import com.ds.session.session_service.app_context.repositories.DeliveryAssignmentRepository;
import com.ds.session.session_service.app_context.repositories.DeliverySessionRepository;
import com.ds.session.session_service.application.client.parcelclient.ParcelServiceClient;
import com.ds.session.session_service.application.client.parcelclient.response.ParcelResponse;
import com.ds.session.session_service.application.client.zoneclient.ZoneServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.ds.session.session_service.application.client.zoneclient.request.VRPAssignmentRequest;
import com.ds.session.session_service.application.client.zoneclient.response.BaseResponse;
import com.ds.session.session_service.application.client.zoneclient.response.VRPAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.request.AutoAssignmentRequest;
import com.ds.session.session_service.common.entities.dto.request.ManualAssignmentRequest;
import com.ds.session.session_service.common.entities.dto.response.AutoAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.ManualAssignmentResponse;
import com.ds.session.session_service.common.enums.AssignmentStatus;
import com.ds.session.session_service.common.enums.SessionStatus;
import com.ds.session.session_service.common.exceptions.ResourceNotFound;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for admin to manually create delivery assignments
 */
@Service
@Slf4j
@Transactional
public class AdminAssignmentService {
    
    private final DeliveryAssignmentRepository assignmentRepository;
    private final DeliveryAssignmentParcelRepository assignmentParcelRepository;
    private final DeliverySessionRepository sessionRepository;
    private final ParcelServiceClient parcelServiceClient;
    private final ZoneServiceClient zoneServiceClient;
    private final WebClient parcelServiceWebClient;
    private final ObjectMapper objectMapper;
    private final com.ds.session.session_service.application.client.userclient.UserServiceClient userServiceClient;
    
    public AdminAssignmentService(
            DeliveryAssignmentRepository assignmentRepository,
            DeliveryAssignmentParcelRepository assignmentParcelRepository,
            DeliverySessionRepository sessionRepository,
            ParcelServiceClient parcelServiceClient,
            ZoneServiceClient zoneServiceClient,
            @Qualifier("parcelServiceWebClient") WebClient parcelServiceWebClient,
            ObjectMapper objectMapper,
            com.ds.session.session_service.application.client.userclient.UserServiceClient userServiceClient) {
        this.assignmentRepository = assignmentRepository;
        this.assignmentParcelRepository = assignmentParcelRepository;
        this.sessionRepository = sessionRepository;
        this.parcelServiceClient = parcelServiceClient;
        this.zoneServiceClient = zoneServiceClient;
        this.parcelServiceWebClient = parcelServiceWebClient;
        this.objectMapper = objectMapper;
        this.userServiceClient = userServiceClient;
    }
    
    /**
     * Create a manual assignment for a shipper with specified parcels
     * 
     * IMPORTANT: Session must be created first (status CREATED) before calling this method.
     * 
     * Validations:
     * - Session must exist and be in CREATED status
     * - Session's deliveryManId must match request.shipperId
     * - All parcels must have the same delivery address
     * - Parcels must be in shipper's working zones (if zoneId provided)
     * - Parcels must not already be assigned
     * 
     * @param request Manual assignment request (must include sessionId)
     * @return Created assignment response
     */
    public ManualAssignmentResponse createManualAssignment(ManualAssignmentRequest request) {
        log.info("[AdminAssignmentService] Creating manual assignment for session {} with {} parcels", 
            request.getSessionId(), request.getParcelIds().size());
        
        // 1. Fetch and validate session
        DeliverySession session = sessionRepository.findById(UUID.fromString(request.getSessionId()))
            .orElseThrow(() -> new ResourceNotFound("Session not found: " + request.getSessionId()));
        
        if (session.getStatus() != SessionStatus.CREATED) {
            throw new IllegalArgumentException("Session must be in CREATED status. Current status: " + session.getStatus());
        }
        
        if (!session.getDeliveryManId().equals(request.getShipperId())) {
            throw new IllegalArgumentException(
                "Session deliveryManId (" + session.getDeliveryManId() + ") does not match request shipperId (" + request.getShipperId() + ")");
        }
        
        // 2. Fetch all parcels
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
        
        // 6. Create assignment with status PENDING and link to session
        DeliveryAssignment assignment = DeliveryAssignment.builder()
            .session(session) // Link to session immediately
            .shipperId(request.getShipperId())
            .deliveryAddressId(firstDeliveryAddressId)
            .status(AssignmentStatus.PENDING)
            .assignedAt(ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime())
            .parcels(new ArrayList<>())
            .build();
        
        // Link assignment to session (bidirectional relationship)
        session.addAssignment(assignment);
        
        assignment = assignmentRepository.save(assignment);
        log.info("[AdminAssignmentService] Created assignment {} with status PENDING linked to session {}", assignment.getId(), session.getId());
        
        // 7. Create DeliveryAssignmentParcel records
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
        
        // 9. Build response
        return ManualAssignmentResponse.builder()
            .assignmentId(assignment.getId())
            .sessionId(session.getId().toString())
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
     * Sessions handling:
     * - If shipperSessionMap is provided, validate and use those sessions
     * - If shipperSessionMap is null/empty, auto-create CREATED sessions for each assigned shipper
     * - Reuse existing CREATED or IN_PROGRESS sessions if found for a shipper
     * 
     * Algorithm:
     * 1. Fetch shippers and parcels
     * 2. Convert to VRP DTOs
     * 3. Call zone-service VRP API
     * 4. Create assignments and DeliveryAssignmentParcel records from results
     * 5. Group parcels by delivery address (all parcels with same deliveryAddressId in same assignment)
     * 6. Get or create sessions for each shipper (auto-create if not provided)
     * 7. Link assignments to their respective sessions
     * 
     * @param request Auto assignment request (shipperSessionMap is optional - sessions will be auto-created if not provided)
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
        
        // 2. Fetch shippers and filter out those with IN_PROGRESS tasks
        List<VRPAssignmentRequest.VRPShipperDto> shippers = fetchShippers(request.getShipperIds());
        if (shippers.isEmpty()) {
            throw new IllegalArgumentException("No valid shippers found for assignment");
        }
        
        // Filter out shippers that have IN_PROGRESS tasks
        List<String> availableShipperIds = shippers.stream()
            .map(s -> s.getShipperId())
            .filter(shipperId -> {
                // Check if shipper has any IN_PROGRESS assignments (shipperId is String)
                boolean hasInProgressTask = assignmentRepository
                    .findByShipperIdAndStatus(shipperId, AssignmentStatus.IN_PROGRESS, 
                        org.springframework.data.domain.PageRequest.of(0, 1))
                    .hasContent();
                
                if (hasInProgressTask) {
                    log.info("[AdminAssignmentService] Skipping shipper {} - has IN_PROGRESS tasks", shipperId);
                }
                return !hasInProgressTask;
            })
            .collect(Collectors.toList());
        
        // Filter shippers list to only available ones
        shippers = shippers.stream()
            .filter(s -> availableShipperIds.contains(s.getShipperId()))
            .collect(Collectors.toList());
        
        if (shippers.isEmpty()) {
            throw new IllegalArgumentException("No available shippers found (all shippers have IN_PROGRESS tasks)");
        }
        
        log.info("[AdminAssignmentService] {} shippers available for assignment (filtered from initial list)", shippers.size());
        
        // 2b. Merge parcels from existing CREATED sessions
        // For shippers with CREATED sessions, add their existing parcels to the assignment pool
        Set<String> existingParcelIds = new java.util.HashSet<>();
        for (VRPAssignmentRequest.VRPShipperDto shipper : shippers) {
            Optional<DeliverySession> createdSession = sessionRepository
                .findByDeliveryManIdAndStatus(shipper.getShipperId(), SessionStatus.CREATED);
            
            if (createdSession.isPresent()) {
                DeliverySession session = createdSession.get();
                List<DeliveryAssignment> existingAssignments = assignmentRepository
                    .findBySession_Id(session.getId());
                
                // Collect all parcels from existing assignments
                for (DeliveryAssignment assignment : existingAssignments) {
                    List<DeliveryAssignmentParcel> assignmentParcels = assignmentParcelRepository
                        .findByAssignmentId(assignment.getId());
                    for (DeliveryAssignmentParcel ap : assignmentParcels) {
                        existingParcelIds.add(ap.getParcelId());
                    }
                }
                
                if (!existingAssignments.isEmpty()) {
                    log.info("[AdminAssignmentService] Found CREATED session {} for shipper {} with {} assignments. Merging {} parcels into assignment pool.", 
                        session.getId(), shipper.getShipperId(), existingAssignments.size(), existingParcelIds.size());
                }
            }
        }
        
        // Fetch parcels from existing assignments and merge into parcel list
        if (!existingParcelIds.isEmpty()) {
            try {
                List<ParcelResponse> existingParcels = parcelServiceClient.fetchParcelsBulk(
                    existingParcelIds.stream()
                        .map(UUID::fromString)
                        .collect(Collectors.toList())
                ).values().stream()
                    .filter(p -> p != null)
                    .collect(Collectors.toList());
                
                // Add existing parcels to the list (avoid duplicates)
                Set<String> currentParcelIds = parcels.stream()
                    .map(ParcelResponse::getId)
                    .collect(Collectors.toSet());
                
                for (ParcelResponse existingParcel : existingParcels) {
                    if (!currentParcelIds.contains(existingParcel.getId())) {
                        parcels.add(existingParcel);
                        log.debug("[AdminAssignmentService] Added parcel {} from existing CREATED session to assignment pool", 
                            existingParcel.getId());
                    }
                }
                
                log.info("[AdminAssignmentService] Merged {} parcels from existing CREATED sessions into assignment pool", 
                    existingParcels.size());
            } catch (Exception e) {
                log.warn("[AdminAssignmentService] Failed to fetch parcels from existing sessions: {}. Continuing without them.", 
                    e.getMessage());
            }
        }
        
        // Filter out already assigned parcels (now includes parcels from CREATED sessions that will be reassigned)
        parcels = parcels.stream()
            .filter(p -> {
                boolean isAssigned = assignmentParcelRepository.existsByParcelId(p.getId());
                if (isAssigned) {
                    // Check if parcel is in a CREATED session (will be reassigned)
                    // If parcel is already assigned but not in any CREATED session, skip it
                    return existingParcelIds.contains(p.getId());
                }
                return true;
            })
            .collect(Collectors.toList());
        
        if (parcels.isEmpty()) {
            throw new IllegalArgumentException("No valid parcels found for assignment after filtering");
        }
        
        // 3. Convert parcels to VRP orders
        List<VRPAssignmentRequest.VRPOrderDto> orders = parcels.stream()
            .map(this::convertToVRPOrder)
            .filter(o -> o != null)
            .collect(Collectors.toList());
        
        int parcelsWithoutLocation = parcels.size() - orders.size();
        if (orders.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("No valid orders with location information. Total parcels: %d, parcels without location: %d", 
                    parcels.size(), parcelsWithoutLocation));
        }
        
        if (parcelsWithoutLocation > 0) {
            log.warn("[AdminAssignmentService] {} out of {} parcels do not have location information and will be skipped", 
                parcelsWithoutLocation, parcels.size());
        }
        
        log.info("[AdminAssignmentService] Converted {} parcels to {} VRP orders", parcels.size(), orders.size());
        
        // 4. Call zone-service VRP API
        VRPAssignmentRequest vrpRequest = VRPAssignmentRequest.builder()
            .shippers(shippers)
            .orders(orders)
            .vehicle(request.getVehicle() != null ? request.getVehicle() : "motorbike")
            .mode(request.getMode() != null ? request.getMode() : "v2-full")
            .build();
        
        log.info("[AdminAssignmentService] Calling zone-service VRP API with {} shippers, {} orders", 
            shippers.size(), orders.size());
        
        BaseResponse<VRPAssignmentResponse> vrpResponse;
        try {
            vrpResponse = zoneServiceClient.solveVRPAssignment(vrpRequest);
        } catch (Exception e) {
            log.error("[AdminAssignmentService] Exception calling zone-service VRP API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call zone-service VRP API: " + e.getMessage(), e);
        }
        
        log.debug("[AdminAssignmentService] VRP API response: success={}, message={}, result={}", 
            vrpResponse != null ? vrpResponse.getSuccess() : "null",
            vrpResponse != null ? vrpResponse.getMessage() : "null",
            vrpResponse != null && vrpResponse.getResult() != null ? "present" : "null");
        
        if (vrpResponse == null) {
            throw new RuntimeException("Failed to solve VRP assignment: null response from zone-service");
        }
        
        // Zone-service BaseResponse may not have 'success' field, check result instead
        // If result is null and message is present, it's an error
        if (vrpResponse.getResult() == null) {
            // Check if it's an error response
            if (vrpResponse.getMessage() != null && !vrpResponse.getMessage().isEmpty()) {
                log.error("[AdminAssignmentService] VRP API returned error: {}", vrpResponse.getMessage());
                throw new RuntimeException("Failed to solve VRP assignment: " + vrpResponse.getMessage());
            }
            // If success is explicitly false, it's an error
            if (Boolean.FALSE.equals(vrpResponse.getSuccess())) {
                String errorMsg = vrpResponse.getMessage() != null ? vrpResponse.getMessage() : "Unknown error";
                log.error("[AdminAssignmentService] VRP API returned failure: {}", errorMsg);
                throw new RuntimeException("Failed to solve VRP assignment: " + errorMsg);
            }
            // Otherwise, if result is null, it's an error
            log.error("[AdminAssignmentService] VRP API response has null result. Success: {}, Message: {}", 
                vrpResponse.getSuccess(), vrpResponse.getMessage());
            throw new RuntimeException("Failed to solve VRP assignment: result is null");
        }
        
        // If we have result, consider it success (even if success field is null)
        // This handles zone-service BaseResponse format which may not include 'success' field
        if (Boolean.FALSE.equals(vrpResponse.getSuccess())) {
            // Explicit failure
            String errorMsg = vrpResponse.getMessage() != null ? vrpResponse.getMessage() : "Unknown error";
            log.error("[AdminAssignmentService] VRP API returned failure despite having result: {}", errorMsg);
            throw new RuntimeException("Failed to solve VRP assignment: " + errorMsg);
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
        
        // 6. Get or create sessions for each shipper
        // Collect all unique shipper IDs from VRP assignments
        Set<String> assignedShipperIds = shipperAddressParcels.keySet();
        Map<String, DeliverySession> shipperSessionMap = new HashMap<>();
        
        // If shipperSessionMap is provided, validate and use it
        if (request.getShipperSessionMap() != null && !request.getShipperSessionMap().isEmpty()) {
        for (Map.Entry<String, String> entry : request.getShipperSessionMap().entrySet()) {
            String shipperId = entry.getKey();
            String sessionId = entry.getValue();
            
            DeliverySession session = sessionRepository.findById(UUID.fromString(sessionId))
                .orElseThrow(() -> new ResourceNotFound("Session not found: " + sessionId + " for shipper: " + shipperId));
            
            if (session.getStatus() != SessionStatus.CREATED) {
                throw new IllegalArgumentException("Session " + sessionId + " must be in CREATED status. Current status: " + session.getStatus());
            }
            
            if (!session.getDeliveryManId().equals(shipperId)) {
                throw new IllegalArgumentException("Session " + sessionId + " deliveryManId (" + session.getDeliveryManId() + ") does not match shipperId (" + shipperId + ")");
            }
            
            shipperSessionMap.put(shipperId, session);
            }
        }
        
        // Auto-create sessions for shippers that don't have one yet
        // IMPORTANT: Only reuse CREATED sessions, not IN_PROGRESS (shippers with IN_PROGRESS were already filtered out)
        for (String shipperId : assignedShipperIds) {
            if (!shipperSessionMap.containsKey(shipperId)) {
                // Check if shipper already has a CREATED session
                Optional<DeliverySession> existingCreatedSession = sessionRepository
                    .findByDeliveryManIdAndStatus(shipperId, SessionStatus.CREATED);
                
                if (existingCreatedSession.isPresent()) {
                    log.info("[AdminAssignmentService] Reusing existing CREATED session {} for shipper {}", 
                        existingCreatedSession.get().getId(), shipperId);
                    shipperSessionMap.put(shipperId, existingCreatedSession.get());
                    
                    // Delete existing assignments in this session (they will be reassigned by VRP)
                    DeliverySession session = existingCreatedSession.get();
                    List<DeliveryAssignment> existingAssignments = assignmentRepository
                        .findBySession_Id(session.getId());
                    
                    if (!existingAssignments.isEmpty()) {
                        log.info("[AdminAssignmentService] Removing {} existing assignments from session {} for reassignment", 
                            existingAssignments.size(), session.getId());
                        
                        for (DeliveryAssignment assignment : existingAssignments) {
                            // Delete assignment parcels first (foreign key constraint)
                            List<DeliveryAssignmentParcel> assignmentParcels = assignmentParcelRepository
                                .findByAssignmentId(assignment.getId());
                            assignmentParcelRepository.deleteAll(assignmentParcels);
                            // Then delete assignment
                            assignmentRepository.delete(assignment);
                        }
                    }
                } else {
                    // Create new CREATED session for this shipper (shipper has no CREATED session)
                    log.info("[AdminAssignmentService] Auto-creating CREATED session for shipper {} (no existing CREATED session)", shipperId);
                    DeliverySession newSession = DeliverySession.builder()
                        .deliveryManId(shipperId)
                        .status(SessionStatus.CREATED)
                        .startTime(ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime())
                        .build();
                    DeliverySession savedSession = sessionRepository.save(newSession);
                    shipperSessionMap.put(shipperId, savedSession);
                    log.info("[AdminAssignmentService] Created session {} for shipper {}", savedSession.getId(), shipperId);
                }
            }
        }
        
        // 7. Create DeliveryAssignment records grouped by delivery address
        List<AutoAssignmentResponse.AssignmentInfo> allAssignments = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, List<String>>> shipperEntry : shipperAddressParcels.entrySet()) {
            String shipperId = shipperEntry.getKey();
            Map<String, List<String>> addressParcels = shipperEntry.getValue();
            
            // Get session for this shipper (should always exist after auto-creation step)
            DeliverySession session = shipperSessionMap.get(shipperId);
            if (session == null) {
                throw new IllegalStateException("Session not found for shipper: " + shipperId + ". This should not happen after auto-creation step.");
            }
            
            List<AutoAssignmentResponse.AssignmentInfo> shipperAssignments = new ArrayList<>();
            
            for (Map.Entry<String, List<String>> addressEntry : addressParcels.entrySet()) {
                String deliveryAddressId = addressEntry.getKey();
                List<String> parcelIds = addressEntry.getValue();
                
                // Create assignment with session linked
                DeliveryAssignment assignment = DeliveryAssignment.builder()
                    .session(session) // Link to session immediately
                    .shipperId(shipperId)
                    .deliveryAddressId(deliveryAddressId)
                    .status(AssignmentStatus.PENDING)
                    .assignedAt(ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDateTime())
                    .parcels(new ArrayList<>())
                    .build();
                
                // Link assignment to session (bidirectional relationship)
                session.addAssignment(assignment);
                
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
     * Fetch parcels from parcel-service (optimized with bulk API)
     * If parcelIds is null/empty, fetches all unassigned parcels with status IN_WAREHOUSE, DELAYED, or ON_ROUTE
     */
    private List<ParcelResponse> fetchParcels(List<String> parcelIds) {
        if (parcelIds == null || parcelIds.isEmpty()) {
            // Fetch all unassigned parcels from parcel-service with status IN_WAREHOUSE, DELAYED, ON_ROUTE
            log.info("[AdminAssignmentService] Fetching all unassigned parcels with eligible statuses");
            return fetchUnassignedParcels();
        }
        
        try {
            List<UUID> parcelUuids = parcelIds.stream()
                .map(UUID::fromString)
                .collect(Collectors.toList());
            Map<String, ParcelResponse> parcelMap = parcelServiceClient.fetchParcelsBulk(parcelUuids);
            return new ArrayList<>(parcelMap.values());
        } catch (Exception e) {
            log.error("[AdminAssignmentService] Failed to fetch parcels in bulk: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Fetch all unassigned parcels with status IN_WAREHOUSE, DELAYED, or ON_ROUTE from parcel-service V2 API
     */
    private List<ParcelResponse> fetchUnassignedParcels() {
        try {
            // Build V2 API request with filter for status IN (IN_WAREHOUSE, DELAYED, ON_ROUTE)
            Map<String, Object> requestBody = new HashMap<>();
            
            // Build filter: status IN (IN_WAREHOUSE, DELAYED, ON_ROUTE)
            Map<String, Object> statusFilter = new HashMap<>();
            statusFilter.put("type", "condition");
            statusFilter.put("field", "status");
            statusFilter.put("operator", "IN");
            statusFilter.put("value", List.of("IN_WAREHOUSE", "DELAYED", "ON_ROUTE"));
            
            Map<String, Object> filters = new HashMap<>();
            filters.put("type", "group");
            filters.put("operator", "AND");
            filters.put("items", List.of(statusFilter));
            
            requestBody.put("filters", filters);
            requestBody.put("page", 0);
            requestBody.put("size", 1000); // Request up to 1000 parcels at once
            
            log.debug("[AdminAssignmentService] Calling parcel-service V2 API to fetch unassigned parcels");
            
            String responseBody = parcelServiceWebClient.post()
                .uri("/api/v2/parcels")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            if (responseBody == null || responseBody.isBlank()) {
                log.warn("[AdminAssignmentService] Empty response from parcel-service V2 API");
                return new ArrayList<>();
            }
            
            // Parse response
            JsonNode responseJson = objectMapper.readTree(responseBody);
            JsonNode resultNode = responseJson.get("result");
            if (resultNode == null) {
                log.warn("[AdminAssignmentService] Invalid response format from parcel-service V2 API: missing 'result'");
                return new ArrayList<>();
            }
            
            JsonNode dataNode = resultNode.get("data");
            if (dataNode == null || !dataNode.isArray()) {
                log.warn("[AdminAssignmentService] Invalid response format from parcel-service V2 API: missing or invalid 'data' array");
                return new ArrayList<>();
            }
            
            // Convert JSON array to List<ParcelResponse>
            List<ParcelResponse> parcels = new ArrayList<>();
            for (JsonNode parcelNode : dataNode) {
                try {
                    ParcelResponse parcel = objectMapper.treeToValue(parcelNode, ParcelResponse.class);
                    if (parcel != null) {
                        parcels.add(parcel);
                    }
                } catch (Exception e) {
                    log.warn("[AdminAssignmentService] Failed to parse parcel from response: {}", e.getMessage());
                }
            }
            
            log.info("[AdminAssignmentService] Fetched {} unassigned parcels from parcel-service", parcels.size());
            return parcels;
            
        } catch (Exception e) {
            log.error("[AdminAssignmentService] Failed to fetch unassigned parcels from parcel-service V2 API: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Fetch shippers from user-service and convert to VRP DTOs
     */
    private List<VRPAssignmentRequest.VRPShipperDto> fetchShippers(List<String> shipperIds) {
        List<VRPAssignmentRequest.VRPShipperDto> shippers = new ArrayList<>();
        List<com.ds.session.session_service.application.client.userclient.response.DeliveryManResponse> deliveryMen;
        
        if (shipperIds == null || shipperIds.isEmpty()) {
            // Fetch all available delivery men from user-service
            log.info("[AdminAssignmentService] Fetching all delivery men from user-service");
            deliveryMen = userServiceClient.getAllDeliveryMen();
        } else {
            // Fetch specific delivery men by IDs
            log.info("[AdminAssignmentService] Fetching {} delivery men from user-service", shipperIds.size());
            Map<String, com.ds.session.session_service.application.client.userclient.response.DeliveryManResponse> deliveryMenMap = 
                userServiceClient.getDeliveryMenByUserIds(shipperIds);
            deliveryMen = new ArrayList<>(deliveryMenMap.values());
        }
        
        if (deliveryMen.isEmpty()) {
            log.warn("[AdminAssignmentService] No delivery men found from user-service");
            return shippers;
        }
        
        // Convert DeliveryManResponse to VRPShipperDto
        for (com.ds.session.session_service.application.client.userclient.response.DeliveryManResponse dm : deliveryMen) {
            if (dm.getUserId() == null || dm.getUserId().isEmpty()) {
                log.warn("[AdminAssignmentService] Skipping delivery man with null/empty userId");
                continue;
            }
            
            // Determine vehicle type from delivery man data or use default
            String vehicleType = "motorbike"; // Default
            if (dm.getVehicleType() != null && !dm.getVehicleType().isEmpty()) {
                String vt = dm.getVehicleType().toLowerCase();
                if (vt.contains("car") || vt.contains("xe") || vt.contains("ô tô")) {
                    vehicleType = "car";
                } else {
                    vehicleType = "motorbike";
                }
            }
            
            // Determine capacity from delivery man data or use default
            Integer capacity = 10; // Default capacity (number of parcels)
            if (dm.getCapacityKg() != null && dm.getCapacityKg() > 0) {
                // Estimate capacity based on weight: assume average parcel is 2kg
                capacity = Math.max(1, (int) (dm.getCapacityKg() / 2.0));
            }
            
            // Calculate maxSessionTime based on current time:
            // - If before 12:00 PM: 2 hours
            // - If after 12:00 PM: from now to 17:30 + 30 minutes buffer (to 18:00)
            double maxSessionTime = calculateMaxSessionTime();
            
            // Get shift start time (current time, or 8:00 AM if before 8:00 or after 21:00)
            String shiftStart = calculateShiftStart();
            
            // Get working zone IDs from delivery man
            List<String> zoneIds = dm.getZoneIds() != null ? dm.getZoneIds() : new ArrayList<>();
            
            VRPAssignmentRequest.VRPShipperDto shipper = VRPAssignmentRequest.VRPShipperDto.builder()
                .shipperId(dm.getUserId())
                .lat(10.82398098) // Default location (Ho Chi Minh City) - TODO: Get from delivery man location
                .lon(106.79611036)
                .shiftStart(shiftStart)
                .maxSessionTime(maxSessionTime)
                .capacity(capacity)
                .zoneIds(zoneIds)
                .vehicle(vehicleType)
                .build();
            shippers.add(shipper);
        }
        
        log.info("[AdminAssignmentService] Fetched {} shippers for VRP assignment", shippers.size());
        return shippers;
    }
    
    /**
     * Calculate shift start time based on current time (UTC+7):
     * - If current time is before 8:00 AM or after 21:00 (9 PM), use 8:00 AM
     * - Otherwise, use current time
     * 
     * @return Shift start time in HH:mm:ss format
     */
    private String calculateShiftStart() {
        // Use UTC+7 (Vietnam timezone)
        ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDateTime now = ZonedDateTime.now(vietnamZone).toLocalDateTime();
        LocalTime currentTime = now.toLocalTime();
        LocalTime morningStart = LocalTime.of(8, 0); // 8:00 AM
        LocalTime eveningEnd = LocalTime.of(21, 0); // 9:00 PM (21:00)
        
        if (currentTime.isBefore(morningStart) || currentTime.isAfter(eveningEnd)) {
            log.debug("[AdminAssignmentService] Current time {} (UTC+7) is before 8:00 AM or after 21:00. Using shiftStart = 08:00:00", currentTime);
            return "08:00:00";
        } else {
            String shiftStart = String.format("%02d:%02d:%02d", currentTime.getHour(), currentTime.getMinute(), currentTime.getSecond());
            log.debug("[AdminAssignmentService] Using current time {} (UTC+7) as shiftStart", shiftStart);
            return shiftStart;
        }
    }
    
    /**
     * Calculate max session time based on current time (UTC+7):
     * - If before 12:00 PM: 2 hours
     * - If after 12:00 PM: time from now to 17:30 + 30 minutes buffer (to 18:00)
     * 
     * Total delivery time = travel time + service time (5 minutes per parcel when arriving at destination)
     * 
     * @return Max session time in hours
     */
    private double calculateMaxSessionTime() {
        // Use UTC+7 (Vietnam timezone)
        ZoneId vietnamZone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDateTime now = ZonedDateTime.now(vietnamZone).toLocalDateTime();
        LocalTime currentTime = now.toLocalTime();
        LocalTime noon = LocalTime.of(12, 0);
        LocalTime deliveryEndTime = LocalTime.of(17, 30); // 17:30
        LocalTime deliveryBufferEndTime = LocalTime.of(18, 0); // 18:00 (17:30 + 30 min buffer)
        
        if (currentTime.isBefore(noon)) {
            // Before 12:00 PM: 2 hours
            log.debug("[AdminAssignmentService] Current time {} (UTC+7) is before 12:00 PM. Using maxSessionTime = 2 hours", currentTime);
            return 2.0;
        } else {
            // After 12:00 PM: calculate time from now to 17:30 + 30 minutes buffer
            LocalDateTime deliveryEndDateTime = now.toLocalDate().atTime(deliveryEndTime);
            LocalDateTime deliveryBufferEndDateTime = now.toLocalDate().atTime(deliveryBufferEndTime);
            
            // If current time is already after 17:30, use 30 minutes buffer
            if (now.isAfter(deliveryEndDateTime)) {
                if (now.isAfter(deliveryBufferEndDateTime)) {
                    log.warn("[AdminAssignmentService] Current time {} is after 18:00. Using maxSessionTime = 0.5 hours (buffer only)", currentTime);
                    return 0.5; // 30 minutes minimum
                }
                // Between 17:30 and 18:00: remaining time to 18:00
                java.time.Duration remaining = java.time.Duration.between(now, deliveryBufferEndDateTime);
                double remainingHours = remaining.toMinutes() / 60.0;
                log.debug("[AdminAssignmentService] Current time {} is after 17:30. Using maxSessionTime = {} hours (to 18:00)", 
                    currentTime, remainingHours);
                return Math.max(0.5, remainingHours); // At least 30 minutes
            } else {
                // Before 17:30: time from now to 17:30 + 30 minutes buffer (to 18:00)
                java.time.Duration toDeliveryEnd = java.time.Duration.between(now, deliveryBufferEndDateTime);
                double totalHours = toDeliveryEnd.toMinutes() / 60.0;
                log.debug("[AdminAssignmentService] Current time {} is after 12:00 PM but before 17:30. Using maxSessionTime = {} hours (to 18:00)", 
                    currentTime, totalHours);
                return totalHours;
            }
        }
    }
    
    /**
     * Convert ParcelResponse to VRPOrderDto
     * Tries to get location from:
     * 1. Direct lat/lon fields
     * 2. receiverAddress.lat/lon (from parcel-service V2 API)
     * 
     * Service time: 5 minutes (300 seconds) per parcel when arriving at destination
     */
    private VRPAssignmentRequest.VRPOrderDto convertToVRPOrder(ParcelResponse parcel) {
        BigDecimal lat = parcel.getLat();
        BigDecimal lon = parcel.getLon();
        
        // If no direct lat/lon, try to get from receiverAddress
        if ((lat == null || lon == null) && parcel.getReceiverAddress() != null) {
            ParcelResponse.AddressInfoDto receiverAddress = parcel.getReceiverAddress();
            lat = receiverAddress.getLat();
            lon = receiverAddress.getLon();
        }
        
        if (lat == null || lon == null) {
            log.warn("[AdminAssignmentService] Parcel {} does not have location information (neither direct lat/lon nor receiverAddress)", parcel.getId());
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
        
        // Get zoneId from receiverAddress if available
        String zoneId = null;
        if (parcel.getReceiverAddress() != null) {
            zoneId = parcel.getReceiverAddress().getZoneId();
        }
        
        return VRPAssignmentRequest.VRPOrderDto.builder()
            .orderId(parcel.getId())
            .lat(lat.doubleValue())
            .lon(lon.doubleValue())
            .serviceTime(300) // Default 5 minutes service time
            .priority(priority)
            .zoneId(zoneId)
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
