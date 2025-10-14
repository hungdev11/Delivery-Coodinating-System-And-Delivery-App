package com.ds.session.session_service.business.v1.services;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ds.session.session_service.app_context.models.DeliveryAssignment;
import com.ds.session.session_service.app_context.repositories.DeliveryAssignmentRepository;
import com.ds.session.session_service.application.client.parcelclient.ParcelServiceClient;
import com.ds.session.session_service.application.client.parcelclient.response.ParcelResponse;
import com.ds.session.session_service.common.entities.dto.request.RouteInfo;
import com.ds.session.session_service.common.entities.dto.response.DeliveryAssignmentResponse;
import com.ds.session.session_service.common.enums.AssignmentStatus;
import com.ds.session.session_service.common.enums.ParcelEvent;
import com.ds.session.session_service.common.exceptions.ResourceNotFound;
import com.ds.session.session_service.common.interfaces.IDeliveryAssignmentService;
import com.ds.session.session_service.common.mapper.ParcelMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeliveryAssignmentService implements IDeliveryAssignmentService {

    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final ParcelServiceClient parcelServiceClient;
    private final ParcelMapper parcelMapper; 
    private final LocalDateTime beginOfDay = LocalDateTime.MIN;
    private final LocalDateTime endOfDay = beginOfDay.plus(24, ChronoUnit.HOURS);

    @Override
    public boolean acceptTask(UUID parcelId, UUID deliveryManId) {
        try {
            ParcelResponse parcelResponse = parcelServiceClient.fetchParcelResponse(parcelId);
            
            if (parcelResponse.getStatus() == null || !"IN_WAREHOUSE".equals(parcelResponse.getStatus())) {
                log.warn("Cannot take this parcel", parcelId);
                return false;
            }
            // check shipper zone and parcel's destination zone 
            // ....
            // check today, is this parcel is assigned with this shipper?
            if (deliveryAssignmentRepository.existsByDeliveryManIdAndParcelIdAndScanedAtBetween(
                deliveryManId.toString(), 
                parcelId.toString(), 
                beginOfDay, 
                endOfDay))
            {
                log.warn("This parcel already assigned to shipper today", parcelId);
                return false;
            }
            // transit parcel status
            parcelServiceClient.changeParcelStatus(parcelId, ParcelEvent.SCAN_QR);
        } catch (FeignException e) {
            log.error("Failed to fetch ParcelInfo for parcelId {}. HTTP Error: {}", parcelId, e.status());
            return false; 
        }
        // create new and save
        DeliveryAssignment deliveryAssignment = DeliveryAssignment.builder()
            .deliveryManId(deliveryManId.toString())
            .parcelId(parcelId.toString())
            .scanedAt(LocalDateTime.now())
            .status(AssignmentStatus.PROCESSING)
            .build();
        deliveryAssignmentRepository.save(deliveryAssignment);
        return true;
    }

    @Override
    public DeliveryAssignmentResponse completeTask(UUID parcelId, UUID deliveryManId, RouteInfo routeInfo) {
        DeliveryAssignment deliveryAssignment = findAssignmentIn(parcelId.toString(), deliveryManId.toString(), beginOfDay, endOfDay);
        insertRouteInfo(deliveryAssignment, routeInfo);
        if (!AssignmentStatus.PROCESSING.equals(deliveryAssignment.getStatus())) {
            throw new RuntimeException("Can not finish assignment not processing");
        }
        ParcelInfo parcel = parcelMapper.toParcelInfo(parcelServiceClient.changeParcelStatus(parcelId, ParcelEvent.DELIVERY_SUCCESSFUL));
        deliveryAssignment.setStatus(AssignmentStatus.SUCCESS);
        deliveryAssignment.setFailReason("");

        deliveryAssignmentRepository.save(deliveryAssignment);
        return DeliveryAssignmentResponse.from(deliveryAssignment, parcel, "Phan Phi Hung", "Maria Akamoto");
    }

    @Override
    public DeliveryAssignmentResponse deliveryFailed(UUID parcelId, UUID deliveryManId, String reason, RouteInfo routeInfo) {
        DeliveryAssignment deliveryAssignment = findAssignmentIn(parcelId.toString(), deliveryManId.toString(), beginOfDay, endOfDay);
        insertRouteInfo(deliveryAssignment, routeInfo);
        if (!AssignmentStatus.PROCESSING.equals(deliveryAssignment.getStatus())) {
            throw new RuntimeException("Can not finish assignment not processing");
        }
        ParcelInfo parcel = parcelMapper.toParcelInfo(parcelServiceClient.changeParcelStatus(parcelId, ParcelEvent.CAN_NOT_DELIVERY));
        deliveryAssignment.setStatus(AssignmentStatus.FAILED);
        deliveryAssignment.setFailReason(reason);

        deliveryAssignmentRepository.save(deliveryAssignment);
        return DeliveryAssignmentResponse.from(deliveryAssignment, parcel, "Phan Phi Hung", "Maria Akamoto");
    }

    private void insertRouteInfo(DeliveryAssignment deliveryAssignment, RouteInfo routeInfo) {
        // insert route info even success or fail
        // validate route info
        
        deliveryAssignment.setDistanceM(routeInfo.getDistanceM());
        deliveryAssignment.setDurationS(routeInfo.getDurationS());
        deliveryAssignment.setWaypoints(toJson(routeInfo.getWaypoints()));

        deliveryAssignmentRepository.save(deliveryAssignment);
    }

    private String toJson(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize route info", e);
        }
    }

    private DeliveryAssignment findAssignmentIn(String parcelId, String deliveryManId, LocalDateTime start, LocalDateTime end) {
        return deliveryAssignmentRepository.findByDeliveryManIdAndParcelIdAndScanedAtBetween(parcelId, deliveryManId, start, end)
                .orElseThrow(() -> new ResourceNotFound("Delivery assignment not found"));
    }
}