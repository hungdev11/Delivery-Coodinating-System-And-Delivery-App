package com.ds.session.session_service.application.controllers.v0;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds.session.session_service.common.entities.dto.request.PagingRequestV0;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;
import com.ds.session.session_service.common.entities.dto.response.PageResponse;
import com.ds.session.session_service.common.interfaces.IDeliveryAssignmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * V0 API Controller for Delivery Assignments
 * V0: Simple paging and sorting without dynamic filters
 */
@RestController
@RequestMapping("/api/v0/assignments")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DeliveryAssignmentControllerV0 {

    private final IDeliveryAssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<PageResponse<DeliveryAssignmentResponse>> getAssignments(
        @Valid @RequestBody PagingRequestV0 request
    ) {
        log.debug("[session-service] [DeliveryAssignmentControllerV0.getAssignments] POST /api/v0/assignments - Get assignments with simple paging (V0)");
        PageResponse<DeliveryAssignmentResponse> response = assignmentService.getDailyTasksV0(request);
        return ResponseEntity.ok(response);
    }
}
