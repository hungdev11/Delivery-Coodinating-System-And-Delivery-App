package com.ds.session.session_service.application.controllers.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds.session.session_service.common.entities.dto.request.PagingRequestV2;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.PageResponse;
import com.ds.session.session_service.common.interfaces.IDeliveryAssignmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * V2 API Controller for Delivery Assignments
 * V2: Enhanced dynamic filtering with operations between each pair of conditions
 */
@RestController
@RequestMapping("/api/v2/assignments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DeliveryAssignmentControllerV2 {

    private final IDeliveryAssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<PageResponse<DeliveryAssignmentResponse>> getAssignments(
        @Valid @RequestBody PagingRequestV2 request
    ) {
        log.info("POST /api/v2/assignments - Get assignments with enhanced filtering (V2)");
        PageResponse<DeliveryAssignmentResponse> response = assignmentService.getDailyTasksV2(request);
        return ResponseEntity.ok(response);
    }
}
