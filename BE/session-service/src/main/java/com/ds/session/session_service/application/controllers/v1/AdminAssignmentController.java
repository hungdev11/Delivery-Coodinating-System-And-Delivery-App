package com.ds.session.session_service.application.controllers.v1;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds.session.session_service.business.v1.services.AdminAssignmentService;
import com.ds.session.session_service.common.entities.dto.common.BaseResponse;
import com.ds.session.session_service.common.entities.dto.request.AutoAssignmentRequest;
import com.ds.session.session_service.common.entities.dto.request.ManualAssignmentRequest;
import com.ds.session.session_service.common.entities.dto.response.AutoAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.ManualAssignmentResponse;
import com.ds.session.session_service.common.interfaces.ISessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for admin to manage delivery assignments
 */
@RestController
@RequestMapping("/api/v1/admin/assignments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AdminAssignmentController {
    
    private final AdminAssignmentService adminAssignmentService;
    private final ISessionService sessionService;
    
    /**
     * Create a manual assignment for a shipper
     * 
     * POST /api/v1/admin/assignments/manual
     * 
     * Request body:
     * {
     *   "shipperId": "shipper-uuid",
     *   "parcelIds": ["parcel1-uuid", "parcel2-uuid"],
     *   "zoneId": "zone-uuid" // optional
     * }
     * 
     * Validations:
     * - All parcels must have the same delivery address
     * - Parcels must be in shipper's working zones (if zoneId provided)
     * - Parcels must not already be assigned
     * 
     * @param request Manual assignment request
     * @return Created assignment response
     */
    @PostMapping("/manual")
    public ResponseEntity<BaseResponse<ManualAssignmentResponse>> createManualAssignment(
            @Valid @RequestBody ManualAssignmentRequest request) {
        log.info("[AdminAssignmentController] Creating manual assignment for shipper {} with {} parcels", 
            request.getShipperId(), request.getParcelIds().size());
        
        try {
            ManualAssignmentResponse response = adminAssignmentService.createManualAssignment(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.error("[AdminAssignmentController] Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("[AdminAssignmentController] Error creating manual assignment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error("Failed to create manual assignment: " + e.getMessage()));
        }
    }
    
    /**
     * Create auto assignments using VRP solver
     * 
     * POST /api/v1/admin/assignments/auto
     * 
     * Request body:
     * {
     *   "shipperIds": ["shipper1-uuid", "shipper2-uuid"], // optional, if empty uses all available shippers
     *   "parcelIds": ["parcel1-uuid", "parcel2-uuid"], // optional, if empty uses all unassigned parcels
     *   "vehicle": "motorbike", // optional, default: "motorbike"
     *   "mode": "v2-full" // optional, default: "v2-full"
     * }
     * 
     * Uses VRP solver from zone-service to optimize assignments with:
     * - Workload balancing
     * - Session time limits
     * - Zone-based filtering
     * - P0 parcel prioritization
     * 
     * @param request Auto assignment request
     * @return Auto assignment response with created assignments
     */
    @PostMapping("/auto")
    public ResponseEntity<BaseResponse<AutoAssignmentResponse>> createAutoAssignment(
            @Valid @RequestBody AutoAssignmentRequest request) {
        log.info("[AdminAssignmentController] Creating auto assignment");
        
        try {
            AutoAssignmentResponse response = adminAssignmentService.createAutoAssignment(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.error("[AdminAssignmentController] Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("[AdminAssignmentController] Error creating auto assignment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error("Failed to create auto assignment: " + e.getMessage()));
        }
    }
    
    /**
     * Create a delivery session with assignments (admin workflow)
     * 
     * POST /api/v1/admin/sessions
     * 
     * Request body:
     * {
     *   "deliveryManId": "shipper-uuid",
     *   "assignmentsIds": ["assignment1-uuid", "assignment2-uuid"]
     * }
     * 
     * This creates a session and assigns the specified assignments to it.
     * Assignments must be in PENDING or ACCEPTED status.
     * When session is created, assignments are set to IN_PROGRESS and parcels to ON_ROUTE.
     * 
     * @param request Create session request with assignments
     * @return Created session response
     */
    @PostMapping("/sessions")
    public ResponseEntity<BaseResponse<com.ds.session.session_service.common.entities.dto.response.SessionResponse>> createSessionWithAssignments(
            @Valid @RequestBody com.ds.session.session_service.common.entities.dto.request.CreateSessionRequest request) {
        log.info("[AdminAssignmentController] Creating session for shipper {} with {} assignments", 
            request.getDeliveryManId(), request.getAssignmentsIds().size());
        
        try {
            com.ds.session.session_service.common.entities.dto.response.SessionResponse response = 
                sessionService.createSession(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.error("[AdminAssignmentController] Validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(BaseResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("[AdminAssignmentController] Error creating session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error("Failed to create session: " + e.getMessage()));
        }
    }
}
