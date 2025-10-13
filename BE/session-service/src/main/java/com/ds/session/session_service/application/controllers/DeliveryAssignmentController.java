package com.ds.session.session_service.application.controllers;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.request.UpdateTaskRequest;
import com.ds.session.session_service.common.entities.dto.response.PageResponse;
import com.ds.session.session_service.common.interfaces.IDeliveryAssignmentService;
import com.ds.session.session_service.common.utils.Utility;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class DeliveryAssignmentController {

    private final IDeliveryAssignmentService sessionService;

    // @PostMapping("/{taskId}/accept")
    // public ResponseEntity<Void> acceptTask(@PathVariable String taskId,
    //                                        @RequestParam String deliveryManId) {
    //     sessionService.acceptTask(Utility.toUUID(taskId), Utility.toUUID(deliveryManId));
    //     return ResponseEntity.ok().build();
    // }

    // @PostMapping("/{taskId}/complete")
    // public ResponseEntity<TaskSessionResponse> completeTask(@PathVariable String taskId,
    //                                                         @RequestParam String deliveryManId,
    //                                                         @RequestBody RouteInfo routeInfo) {
    //     return ResponseEntity.ok(sessionService.completeTask(Utility.toUUID(taskId), Utility.toUUID(deliveryManId), routeInfo));
    // }

    // @PostMapping("/{taskId}/fail")
    // public ResponseEntity<TaskSessionResponse> deliveryFailed(@PathVariable String taskId,
    //                                                           @RequestParam String deliveryManId,
    //                                                           @RequestParam String reason,
    //                                                           @RequestBody RouteInfo routeInfo) {
    //     return ResponseEntity.ok(sessionService.deliveryFailed(Utility.toUUID(taskId), Utility.toUUID(deliveryManId), reason, routeInfo));
    // }

    // @PatchMapping("/{taskId}/time-window")
    // public ResponseEntity<TaskSessionResponse> changeTimeWindow(@PathVariable String taskId,
    //                                                             @RequestParam String deliveryManId,
    //                                                             @RequestParam
    //                                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    //                                                             LocalDateTime startTime,
    //                                                             @RequestParam
    //                                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    //                                                             LocalDateTime endTime) {
    //     return ResponseEntity.ok(sessionService.changeTimeWindow(Utility.toUUID(taskId), Utility.toUUID(deliveryManId), startTime, endTime));
    // }

    // @GetMapping("/delivery-man/{deliveryManId}/tasks")
    // public ResponseEntity<PageResponse<?>> getTasksOfDeliveryMan(
    //         @PathVariable String deliveryManId,
    //         @RequestParam
    //         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    //         LocalDateTime beginTime,
    //         @RequestParam
    //         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    //         LocalDateTime endTime,
    //         @RequestParam(defaultValue = "0") int page,
    //         @RequestParam(defaultValue = "10") int size,
    //         @RequestParam(defaultValue = "assignedAt") String sortBy,
    //         @RequestParam(defaultValue = "desc") String direction) {

    //     return ResponseEntity.ok(
    //             sessionService.getTasksOfDeliveryMan(Utility.toUUID(deliveryManId), beginTime, endTime, page, size, sortBy, direction)
    //     );
    // }

    // @GetMapping("/delivery-man/{deliveryManId}/tasks/today")
    // public ResponseEntity<PageResponse<?>> getTaskTodayOfDeliveryMan(
    //         @PathVariable String deliveryManId,
    //         @RequestParam(defaultValue = "0") int page,
    //         @RequestParam(defaultValue = "10") int size,
    //         @RequestParam(defaultValue = "assignedAt") String sortBy,
    //         @RequestParam(defaultValue = "desc") String direction) {

    //     return ResponseEntity.ok(
    //             sessionService.getTaskTodayOfDeliveryMan(Utility.toUUID(deliveryManId), page, size, sortBy, direction)
    //     );
    // }
}