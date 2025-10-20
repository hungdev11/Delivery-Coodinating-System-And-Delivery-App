package com.ds.session.session_service.application.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;
import com.ds.session.session_service.common.interfaces.IDeliveryAssignmentService;
import com.ds.session.session_service.common.utils.Utility;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
public class DeliveryAssignmentController {

    private final IDeliveryAssignmentService deliveryAssignmentService;

    @PostMapping("/{parcelId}/accept")
    public ResponseEntity<Boolean> acceptTask(@PathVariable String parcelId,
                                              @RequestParam String deliveryManId) {
    
        return ResponseEntity.ok(deliveryAssignmentService.acceptTask(Utility.toUUID(parcelId), Utility.toUUID(deliveryManId)));
    }

    @PutMapping("/{parcelId}/complete")
    public ResponseEntity<DeliveryAssignmentResponse> completeTask(@PathVariable String parcelId,
                                                            @RequestParam String deliveryManId,
                                                            @RequestBody RouteInfo routeInfo) {
        return ResponseEntity.ok(deliveryAssignmentService.completeTask(Utility.toUUID(parcelId), Utility.toUUID(deliveryManId), routeInfo));
    }

    @PutMapping("/{parcelId}/fail")
    public ResponseEntity<DeliveryAssignmentResponse> deliveryFailed(@PathVariable String parcelId,
                                                              @RequestParam String deliveryManId,
                                                              @RequestParam String reason,
                                                              @RequestParam boolean flag, // recognize case delivery success but, parcel false cuz customer reject
                                                              @RequestBody RouteInfo routeInfo) {
        return ResponseEntity.ok(deliveryAssignmentService.deliveryFailed(Utility.toUUID(parcelId), Utility.toUUID(deliveryManId), flag, reason, routeInfo));
    }

    @GetMapping("/today/{deliveryManId}")
    public ResponseEntity<List<DeliveryAssignmentResponse>> getDailyTasks(@PathVariable UUID deliveryManId) {
        return ResponseEntity.ok(deliveryAssignmentService.getDailyTasks(deliveryManId));
    }

    @GetMapping("/{deliveryManId}")
    public ResponseEntity<List<DeliveryAssignmentResponse>> getDailyTasks(
        @PathVariable UUID deliveryManId, 
        @RequestParam(required = false) 
        @DateTimeFormat(pattern = "dd/MM/yyyy")
        LocalDate start, 

        @RequestParam(required = false) 
        @DateTimeFormat(pattern = "dd/MM/yyyy")
        LocalDate end
    ){
        return ResponseEntity.ok(deliveryAssignmentService.getTasksBetween(deliveryManId, start, end));
    }
}