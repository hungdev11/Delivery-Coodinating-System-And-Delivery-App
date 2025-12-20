package com.ds.parcel_service.business.v1.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.ds.parcel_service.app_context.models.Parcel;
import com.ds.parcel_service.app_context.models.ParcelDestination;
import com.ds.parcel_service.application.services.AssignmentService.AssignmentInfo;
import com.ds.parcel_service.app_context.repositories.ParcelDestinationRepository;
import com.ds.parcel_service.app_context.repositories.ParcelRepository;
import com.ds.parcel_service.application.client.UserServiceClient;
import com.ds.parcel_service.application.client.UserServiceClient.UserInfo;
import com.ds.parcel_service.application.client.DesDetail;
import com.ds.parcel_service.application.client.DestinationResponse;
import com.ds.parcel_service.application.client.ZoneClient;
import com.ds.parcel_service.application.client.SessionServiceClient;
import com.ds.parcel_service.application.services.AssignmentService;
import com.ds.parcel_service.common.entities.dto.common.PagedData;
import com.ds.parcel_service.common.entities.dto.request.ParcelCreateRequest;
import com.ds.parcel_service.common.entities.dto.request.ParcelFilterRequest;
import com.ds.parcel_service.common.entities.dto.request.ParcelUpdateRequest;
import com.ds.parcel_service.common.entities.dto.request.ParcelConfirmRequest;
import com.ds.parcel_service.common.entities.dto.request.PagingRequestV2;
import com.ds.parcel_service.common.entities.dto.response.PageResponse;
import com.ds.parcel_service.common.entities.dto.response.ParcelResponse;
import com.ds.parcel_service.common.entities.dto.sort.SortConfig;
import com.ds.parcel_service.common.enums.DeliveryType;
import com.ds.parcel_service.common.enums.DestinationType;
import com.ds.parcel_service.common.enums.ParcelEvent;
import com.ds.parcel_service.common.enums.ParcelStatus;
import com.ds.parcel_service.common.exceptions.ResourceNotFound;
import com.ds.parcel_service.common.interfaces.IParcelService;
import com.ds.parcel_service.common.parcelstates.DelayedState;
import com.ds.parcel_service.common.parcelstates.DeliveredState;
import com.ds.parcel_service.common.parcelstates.DisputeState;
import com.ds.parcel_service.common.parcelstates.FailedState;
import com.ds.parcel_service.common.parcelstates.IParcelState;
import com.ds.parcel_service.common.parcelstates.InWarehouseState;
import com.ds.parcel_service.common.parcelstates.LostState;
import com.ds.parcel_service.common.parcelstates.OnRouteState;
import com.ds.parcel_service.common.parcelstates.SuccededState;
import com.ds.parcel_service.common.utils.EnhancedQueryParserV2;
import com.ds.parcel_service.common.utils.PageUtil;
import com.ds.parcel_service.common.utils.ParcelSpecification;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParcelService implements IParcelService{

    private final ParcelRepository parcelRepository;
    private final ParcelDestinationRepository parcelDestinationRepository;
    private final ZoneClient zoneClient;
    private final UserServiceClient userServiceClient;
    private final AssignmentService assignmentService;
    private final SessionServiceClient sessionServiceClient;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    private final Map<ParcelStatus, IParcelState> stateMap = Map.of(
        ParcelStatus.IN_WAREHOUSE, new InWarehouseState(),
        ParcelStatus.ON_ROUTE, new OnRouteState(),
        ParcelStatus.DELIVERED, new DeliveredState(),
        ParcelStatus.FAILED, new FailedState(),
        ParcelStatus.SUCCEEDED, new SuccededState(),
        ParcelStatus.DELAYED, new DelayedState(),
        ParcelStatus.DISPUTE, new DisputeState(),
        ParcelStatus.LOST, new LostState()
    );

    //Validate state transition path
    private boolean isTransitionValid(ParcelStatus current, ParcelStatus next) {
        return switch (current) {
            case IN_WAREHOUSE -> next == ParcelStatus.ON_ROUTE;
            case ON_ROUTE -> next == ParcelStatus.DELIVERED || next == ParcelStatus.FAILED || next == ParcelStatus.DELAYED;
            case DELIVERED -> next == ParcelStatus.SUCCEEDED || next == ParcelStatus.FAILED || next == ParcelStatus.DISPUTE;
            case DISPUTE -> next == ParcelStatus.SUCCEEDED || next == ParcelStatus.LOST;
            case DELAYED -> next == ParcelStatus.IN_WAREHOUSE;
            case FAILED, SUCCEEDED, LOST -> false; // Last state
            default -> false;
        };
    }

    @Transactional
    private Parcel processTransition(UUID parcelId, ParcelEvent event) {
        Parcel parcel = parcelRepository.findById(parcelId)
            .orElseThrow(() -> new ResourceNotFound("Parcel not found"));

        ParcelStatus currentStatus = parcel.getStatus();
                
        IParcelState currentStateObject = stateMap.get(currentStatus);
        if (currentStateObject == null) {
            log.error("[parcel-service] [ParcelService.processTransition] Missing state handler for status: {}", currentStatus);
            throw new IllegalStateException("Missing state handler for current status.");
        }
        
        ParcelStatus nextStatus = currentStateObject.handleTransition(event);

        if (currentStatus.equals(nextStatus)) {
            log.debug("[parcel-service] [ParcelService.processTransition] Parcel {} state remains {}. Event processed: {}", parcelId, currentStatus, event);
            return parcel; 
        }

        if (!isTransitionValid(parcel.getStatus(), nextStatus)) {
            throw new IllegalStateException("Invalid state transition from " 
                                            + parcel.getStatus() + " to " + nextStatus);
        }

        // mark delivered for background job works
        if (nextStatus == ParcelStatus.DELIVERED) {
            parcel.setDeliveredAt(LocalDateTime.now());
        }
        
        parcel.setStatus(nextStatus);
        Parcel saved = parcelRepository.save(parcel);
        
        // Publish update notification if status changed to SUCCEEDED
        if (nextStatus == ParcelStatus.SUCCEEDED) {
            publishParcelSucceededNotification(saved, null, null); // Auto timeout - no confirmedBy
        }
        
        // Publish update notification for DISPUTE status
        if (nextStatus == ParcelStatus.DISPUTE) {
            publishParcelStatusNotification(saved, "DISPUTE");
        }
        
        // Publish update notification for LOST status
        if (nextStatus == ParcelStatus.LOST) {
            publishParcelStatusNotification(saved, "LOST");
        }
        
        return saved;
    }
    
    /**
     * Publish update notification when parcel status changes to SUCCEEDED
     */
    private void publishParcelSucceededNotification(Parcel parcel, String confirmedBy, LocalDateTime confirmedAt) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("parcelId", parcel.getId().toString());
            data.put("parcelCode", parcel.getCode());
            data.put("status", "SUCCEEDED");
            data.put("receiverId", parcel.getReceiverId() != null ? parcel.getReceiverId().toString() : null);
            data.put("senderId", parcel.getSenderId() != null ? parcel.getSenderId().toString() : null);
            
            // Include confirmedBy and confirmedAt if user confirmed
            if (confirmedBy != null) {
                data.put("confirmedBy", confirmedBy);
                data.put("confirmedAt", confirmedAt != null ? confirmedAt.toString() : LocalDateTime.now().toString());
            }
            
            // Try to get deliveryManId from Session Service
            try {
                AssignmentInfo assignmentInfo = assignmentService.getOrFetch(parcel.getId());
                if (assignmentInfo != null && assignmentInfo.getDeliveryManId() != null) {
                    data.put("deliveryManId", assignmentInfo.getDeliveryManId());
                }
            } catch (Exception e) {
                log.debug("[parcel-service] [ParcelService.publishParcelSucceededNotification] Could not get deliveryManId from Session Service", e);
            }
            
            // Create UpdateNotificationDTO as Map (parcel-service doesn't have communication_service dependency)
            Map<String, Object> updateNotification = new HashMap<>();
            updateNotification.put("id", UUID.randomUUID().toString());
            updateNotification.put("userId", parcel.getReceiverId() != null ? parcel.getReceiverId().toString() : "");
            updateNotification.put("updateType", "PARCEL_UPDATE");
            updateNotification.put("entityType", "PARCEL");
            updateNotification.put("entityId", parcel.getId().toString());
            updateNotification.put("action", "STATUS_CHANGED");
            updateNotification.put("data", data);
            updateNotification.put("timestamp", LocalDateTime.now().toString());
            updateNotification.put("message", String.format("Đơn hàng %s đã chuyển sang trạng thái hoàn thành", 
                    parcel.getCode() != null ? parcel.getCode() : parcel.getId()));
            updateNotification.put("clientType", "ALL");
            
            // Publish to update-notifications topic
            kafkaTemplate.send("update-notifications", parcel.getId().toString(), updateNotification);
            
            log.debug("[parcel-service] [ParcelService.publishParcelSucceededNotification] Published parcel succeeded notification: parcelId={}, confirmedBy={}", 
                    parcel.getId(), confirmedBy);
        } catch (Exception e) {
            log.error("[parcel-service] [ParcelService.publishParcelSucceededNotification] Failed to publish update notification for parcel {}", 
                    parcel.getId(), e);
            // Don't throw - notification failure shouldn't break the transaction
        }
    }
    
    /**
     * Publish update notification for parcel status changes (DISPUTE, LOST, etc.)
     */
    private void publishParcelStatusNotification(Parcel parcel, String status) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("parcelId", parcel.getId().toString());
            data.put("parcelCode", parcel.getCode());
            data.put("status", status);
            data.put("receiverId", parcel.getReceiverId() != null ? parcel.getReceiverId().toString() : null);
            data.put("senderId", parcel.getSenderId() != null ? parcel.getSenderId().toString() : null);
            
            // Try to get deliveryManId from Session Service
            try {
                AssignmentInfo assignmentInfo = assignmentService.getOrFetch(parcel.getId());
                if (assignmentInfo != null && assignmentInfo.getDeliveryManId() != null) {
                    data.put("deliveryManId", assignmentInfo.getDeliveryManId());
                }
            } catch (Exception e) {
                log.debug("[parcel-service] [ParcelService.publishParcelStatusNotification] Could not get deliveryManId from Session Service", e);
            }
            
            // Create UpdateNotificationDTO as Map
            Map<String, Object> updateNotification = new HashMap<>();
            updateNotification.put("id", UUID.randomUUID().toString());
            updateNotification.put("userId", parcel.getReceiverId() != null ? parcel.getReceiverId().toString() : "");
            updateNotification.put("updateType", "PARCEL_UPDATE");
            updateNotification.put("entityType", "PARCEL");
            updateNotification.put("entityId", parcel.getId().toString());
            updateNotification.put("action", "STATUS_CHANGED");
            updateNotification.put("data", data);
            updateNotification.put("timestamp", LocalDateTime.now().toString());
            updateNotification.put("message", String.format("Đơn hàng %s đã chuyển sang trạng thái %s", 
                    parcel.getCode() != null ? parcel.getCode() : parcel.getId(), status));
            updateNotification.put("clientType", "ALL");
            
            // Publish to update-notifications topic
            kafkaTemplate.send("update-notifications", parcel.getId().toString(), updateNotification);
            
            log.debug("[parcel-service] [ParcelService.publishParcelStatusNotification] Published parcel {} status notification: {}", 
                    parcel.getId(), status);
        } catch (Exception e) {
            log.error("[parcel-service] [ParcelService.publishParcelStatusNotification] Failed to publish update notification for parcel {}", 
                    parcel.getId(), e);
            // Don't throw - notification failure shouldn't break the transaction
        }
    }

    @Override
    public ParcelResponse changeParcelStatus(UUID parcelId, ParcelEvent event) {
        return toDto(processTransition(parcelId, event));
    }

    @Override
    @Transactional
    public ParcelResponse confirmParcelByClient(UUID parcelId, String userId, ParcelConfirmRequest request) {
        Parcel parcel = getParcel(parcelId);

        if (!parcel.getReceiverId().equals(userId)) {
            throw new IllegalStateException("User is not the receiver of this parcel");
        }

        if (parcel.getStatus() != ParcelStatus.DELIVERED) {
            throw new IllegalStateException("Parcel must be in DELIVERED state before confirmation");
        }

        AssignmentInfo assignmentInfo = assignmentService.getOrFetch(parcelId);
        if (assignmentInfo == null || assignmentInfo.getAssignmentId() == null || assignmentInfo.getSessionId() == null) {
            assignmentInfo = assignmentService.refreshFromRemote(parcelId);
        }

        Parcel confirmed = processTransition(parcelId, ParcelEvent.CUSTOMER_RECEIVED);
        LocalDateTime confirmedAtTime = java.time.LocalDateTime.now();
        confirmed.setConfirmedAt(confirmedAtTime);
        confirmed.setConfirmedBy(userId);
        confirmed.setConfirmationNote(request.getNote());
        Parcel saved = parcelRepository.save(confirmed);

        try {
            if (assignmentInfo != null && assignmentInfo.getAssignmentId() != null && assignmentInfo.getSessionId() != null) {
                sessionServiceClient.markAssignmentSuccess(assignmentInfo.getSessionId(), assignmentInfo.getAssignmentId(), request.getNote());
            }
        } catch (Exception ex) {
            log.error("[parcel-service] [ParcelService.confirmParcelByClient] Failed to update assignment status for parcel {}", parcelId, ex);
        }
        
        // Publish update notification (user confirmed)
        publishParcelSucceededNotification(saved, userId, confirmedAtTime);

        return toDto(saved);
    }

    @Override
    @Transactional
    public ParcelResponse createParcel(ParcelCreateRequest request) {
        validateUniqueCode(request.getCode());
        
        // Validate destination IDs are provided
        if (request.getSenderDestinationId() == null || request.getSenderDestinationId().isBlank()) {
            throw new IllegalArgumentException("Sender destination ID is required");
        }
        if (request.getReceiverDestinationId() == null || request.getReceiverDestinationId().isBlank()) {
            throw new IllegalArgumentException("Receiver destination ID is required");
        }
        
        Parcel parcel = Parcel.builder()
                            .code(request.getCode())
                            .deliveryType(DeliveryType.valueOf(request.getDeliveryType()))
                            .senderId(request.getSenderId())
                            .receiverId(request.getReceiverId())
                            .receiveFrom(request.getReceiveFrom())
                            .sendTo(request.getSendTo())
                            .status(ParcelStatus.IN_WAREHOUSE) 
                            .weight(request.getWeight())
                            .value(request.getValue())
                            .windowStart(request.getWindowStart())
                            .windowEnd(request.getWindowEnd())
                            .build();
        
        Parcel savedParcel = parcelRepository.save(parcel);

        // Link parcel to receiver destination (PRIMARY) - this is the current destination
        ParcelDestination receiverPd = ParcelDestination.builder()
            .destinationId(request.getReceiverDestinationId())
            .destinationType(DestinationType.PRIMARY)
            .isCurrent(true)
            .isOriginal(true)
            .parcel(savedParcel)
            .build();

        parcelDestinationRepository.save(receiverPd);
        log.debug("[parcel-service] [ParcelService.createParcel] Linked parcel {} to receiver destination {} (PRIMARY, current)", savedParcel.getId(), request.getReceiverDestinationId());

        // Link parcel to sender destination (SECONDARY) - not current
        ParcelDestination senderPd = ParcelDestination.builder()
            .destinationId(request.getSenderDestinationId())
            .destinationType(DestinationType.SECONDARY)
            .isCurrent(false)
            .isOriginal(true)
            .parcel(savedParcel)
            .build();

        parcelDestinationRepository.save(senderPd);
        log.debug("[parcel-service] [ParcelService.createParcel] Linked parcel {} to sender destination {} (SECONDARY)", savedParcel.getId(), request.getSenderDestinationId());
        
        // Validate: Ensure only one destination is current
        validateSingleCurrentDestination(savedParcel);
        
        return toDto(savedParcel);
    }

    @Override
    public ParcelResponse updateParcel(UUID parcelId, ParcelUpdateRequest request) {
        Parcel parcel = parcelRepository.findById(parcelId).orElseThrow(() -> {
            return new ResourceNotFound("Parcel not found");
        });
        
        if (parcel.getStatus() != ParcelStatus.IN_WAREHOUSE) {
             throw new IllegalStateException("Cannot update parcel data after it leaves the warehouse.");
        }
        
        parcel.setWeight(request.getWeight());
        parcel.setValue(request.getValue());
        return toDto(parcelRepository.save(parcel));
    }

    @Override
    public void deleteParcel(UUID parcelId) {
        // [Logic: Thêm kiểm tra trạng thái trước khi cho phép xóa]
        return;
    }

    @Override
    public ParcelResponse getParcelById(UUID parcelId) {
        Parcel parcel = getParcel(parcelId);
        ParcelDestination des = parcelDestinationRepository.findByParcelAndIsCurrentTrue(parcel).orElseThrow(()-> new ResourceNotFound("Not found any current destination by parcel"));
        
        DestinationResponse<DesDetail> call = zoneClient.getDestination(des.getDestinationId());
        return toDtoWithLocation(parcel, call);
    }

    @Override
    public ParcelResponse getParcelByCode(String code) {
        Parcel parcel = parcelRepository.findByCode(code).orElseThrow(()-> new ResourceNotFound("Not found parcel with code " + code));
        ParcelDestination des = parcelDestinationRepository.findByParcelAndIsCurrentTrue(parcel).orElseThrow(()-> new ResourceNotFound("Not found any current destination by parcel"));
        
        DestinationResponse<DesDetail> call = zoneClient.getDestination(des.getDestinationId());
        return toDtoWithLocation(parcel, call);
    }

    @Override
    public PageResponse<ParcelResponse> getParcels(
        ParcelFilterRequest filter, 
        int page, 
        int size, 
        String sortBy,
        String direction
    )  {
        Pageable pageable = PageUtil.build(page, size, sortBy, direction, Parcel.class);
        Specification<Parcel> spec = ParcelSpecification.buildeSpecification(filter);
        Page<Parcel> parcels = parcelRepository.findAll(spec, pageable);
        return PageResponse.from(parcels.map(this::toDto));
    }

    @Override
    public PageResponse<ParcelResponse> getParcelsV0(com.ds.parcel_service.common.entities.dto.request.PagingRequestV0 request) {
        // V0: Simple paging with sorting only, no dynamic filters
        Pageable pageable = PageUtil.build(
            request.getPage(),
            request.getSize(),
            null,
            "DESC",
            Parcel.class
        );
        
        Page<Parcel> parcels = parcelRepository.findAll(pageable);
        return PageResponse.from(parcels.map(this::toDto));
    }

    @Override
    public PageResponse<ParcelResponse> getParcelsV2(PagingRequestV2 request) {
        // V2: Enhanced filtering with operations between each pair
        Specification<Parcel> spec = Specification.where(null);
        
        if (request.getFiltersOrNull() != null) {
            spec = com.ds.parcel_service.common.utils.EnhancedQueryParserV2.parseFilterGroup(
                request.getFiltersOrNull(),
                Parcel.class
            );
        }

        Pageable pageable = PageUtil.build(
            request.getPage(),
            request.getSize(),
            null,
            "DESC",
            Parcel.class
        );

        Page<Parcel> parcels = parcelRepository.findAll(spec, pageable);
        return PageResponse.from(parcels.map(this::toDto));
    }
    
    /**
     * Get parcels V2 with RESTFUL.md compliant response format
     */
    @Override
    public PagedData<ParcelResponse> getParcelsV2Restful(PagingRequestV2 request) {
        PageResponse<ParcelResponse> pageResponse = getParcelsV2(request);
        return convertToPagedData(pageResponse, request);
    }
    
    @Override
    public PagedData<ParcelResponse> getParcelsForReceiver(String receiverId, PagingRequestV2 request) {
        if (!StringUtils.hasText(receiverId)) {
            throw new IllegalArgumentException("receiverId is required");
        }

        Specification<Parcel> spec = (root, query, cb) -> cb.equal(root.get("receiverId"), receiverId);

        if (request.getFiltersOrNull() != null) {
            Specification<Parcel> additionalFilters = EnhancedQueryParserV2.parseFilterGroup(
                request.getFiltersOrNull(),
                Parcel.class
            );
            spec = spec.and(additionalFilters);
        }

        Sort sort = buildSort(request.getSortsOrEmpty());
        Pageable pageable = PageRequest.of(
            Math.max(request.getPage(), 0),
            Math.max(request.getSize(), 1),
            sort
        );

        Page<Parcel> parcels = parcelRepository.findAll(spec, pageable);
        PageResponse<ParcelResponse> pageResponse = PageResponse.from(parcels.map(this::toDto));
        return convertToPagedData(pageResponse, request);
    }
    
    /**
     * Convert PageResponse to PagedData following RESTFUL.md specification
     */
    private PagedData<ParcelResponse> convertToPagedData(
            PageResponse<ParcelResponse> pageResponse,
            PagingRequestV2 request) {
        PagedData.Paging<String> paging = PagedData.Paging.<String>builder()
                .page(pageResponse.page())
                .size(pageResponse.size())
                .totalElements(pageResponse.totalElements())
                .totalPages(pageResponse.totalPages())
                .filters(request.getFiltersOrNull())
                .sorts(request.getSortsOrEmpty())
                .selected(request.getSelectedOrEmpty())
                .build();
        
        return PagedData.<ParcelResponse>builder()
                .data(pageResponse.content())
                .page(paging)
                .build();
    }

    private Sort buildSort(List<SortConfig> sortConfigs) {
        if (sortConfigs == null || sortConfigs.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        List<Sort.Order> orders = new ArrayList<>();
        for (SortConfig config : sortConfigs) {
            if (config == null || !PageUtil.isValidSortFieldDeep(Parcel.class, config.getField())) {
                continue;
            }
            Sort.Direction direction = "DESC".equalsIgnoreCase(config.getDirection())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, config.getField()));
        }

        if (orders.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return Sort.by(orders);
    }

    private ParcelResponse toDto(Parcel parcel) {
        // Get sender and receiver info from User Service
        final String[] senderName = {null};
        final String[] receiverName = {null};
        final String[] receiverPhoneNumber = {null};
        
        try {
            if (parcel.getSenderId() != null) {
                UserInfo senderInfo = userServiceClient.getUserById(parcel.getSenderId());
                if (senderInfo != null) {
                    senderName[0] = senderInfo.getFullName();
                }
            }
            if (parcel.getReceiverId() != null) {
                UserInfo receiverInfo = userServiceClient.getUserById(parcel.getReceiverId());
                if (receiverInfo != null) {
                    receiverName[0] = receiverInfo.getFullName();
                    receiverPhoneNumber[0] = receiverInfo.getPhone();
                }
            }
        } catch (Exception e) {
            log.debug("[parcel-service] [ParcelService.toDto] Could not fetch user info for parcel {}: {}", parcel.getId(), e.getMessage());
        }
        
        final String finalSenderName = senderName[0];
        final String finalReceiverName = receiverName[0];
        final String finalReceiverPhoneNumber = receiverPhoneNumber[0];
        
        return ParcelResponse.builder()
                            .id(parcel.getId().toString())
                            .code(parcel.getCode())
                            .deliveryType(parcel.getDeliveryType())
                            .senderId(parcel.getSenderId())
                            .senderName(finalSenderName)
                            .receiverId(parcel.getReceiverId())
                            .receiverName(finalReceiverName)
                            .receiverPhoneNumber(finalReceiverPhoneNumber)
                            .receiveFrom(parcel.getReceiveFrom())
                            .targetDestination(parcel.getSendTo())
                            .weight(parcel.getWeight())
                            .value(parcel.getValue())
                            .status(parcel.getStatus())
                            .createdAt(parcel.getCreatedAt())
                            .updatedAt(parcel.getUpdatedAt())
                            .deliveredAt(parcel.getDeliveredAt())
                            .confirmedAt(parcel.getConfirmedAt())
                            .confirmedBy(parcel.getConfirmedBy())
                            .confirmationNote(parcel.getConfirmationNote())
                            .windowStart(parcel.getWindowStart())
                            .windowEnd(parcel.getWindowEnd())
                            .priority(parcel.getPriority())
                            .isDelayed(parcel.getIsDelayed())
                            .delayedUntil(parcel.getDelayedUntil())
                            .build();
    }

    private ParcelResponse toDtoWithLocation(Parcel parcel, DestinationResponse<DesDetail> des) {
        ParcelResponse response = toDto(parcel);
        log.debug("[parcel-service] [ParcelService.toDtoWithLocation] before {} {}", des.getResult().getLat(), des.getResult().getLon());
        response.setLat(des.getResult().getLat());
        response.setLon(des.getResult().getLon());
        log.debug("[parcel-service] [ParcelService.toDtoWithLocation] after {} {}", response.getLat(), response.getLon());
        return response;
    }

    private void validateUniqueCode(String code) {
        if (parcelRepository.existsByCode(code)) {
            throw new IllegalStateException("Parcel with code already exists");
        }
    }
    
    /**
     * Validate that only one destination is marked as current for a parcel.
     * This ensures data integrity for destination management.
     */
    private void validateSingleCurrentDestination(Parcel parcel) {
        List<ParcelDestination> currentDestinations = parcelDestinationRepository.findAllByParcelAndIsCurrentTrue(parcel);
        if (currentDestinations.size() > 1) {
            log.error("[parcel-service] [ParcelService.validateSingleCurrentDestination] Data integrity issue: Parcel {} has {} current destinations. Expected 0 or 1.", 
                parcel.getId(), currentDestinations.size());
            // Fix: Set all but the first one to false
            for (int i = 1; i < currentDestinations.size(); i++) {
                currentDestinations.get(i).setCurrent(false);
                parcelDestinationRepository.save(currentDestinations.get(i));
                log.debug("[parcel-service] [ParcelService.validateSingleCurrentDestination] Fixed: Set destination {} to not current for parcel {}", 
                    currentDestinations.get(i).getId(), parcel.getId());
            }
        }
    }

    private Parcel getParcel(UUID id) {
        return parcelRepository.findById(id).orElseThrow(()->{
            return new ResourceNotFound("Parcel not found");
        });
    }

    @Override
    public Map<String, ParcelResponse> fetchParcelsBulk(List<UUID> parcelIds) {
        long startTime = System.currentTimeMillis();
        log.debug("[parcel-service] [ParcelService.fetchParcelsBulk] Starting optimized fetchParcelsBulk for {} parcels", parcelIds.size());
        
        if (parcelIds == null || parcelIds.isEmpty()) {
            log.debug("[parcel-service] [ParcelService.fetchParcelsBulk] No parcel IDs provided");
            return new HashMap<>();
        }
        
        Map<String, ParcelResponse> result = new HashMap<>();
        
        try {
            // Step 1: Fetch all parcels in batch (1 query instead of N)
            log.debug("[parcel-service] [ParcelService.fetchParcelsBulk] Step 1: Fetching {} parcels from database...", parcelIds.size());
            List<Parcel> parcels = parcelRepository.findAllById(parcelIds);
            long dbQueryTime = System.currentTimeMillis() - startTime;
            log.debug("[parcel-service] [ParcelService.fetchParcelsBulk] Fetched {} parcels from database in {}ms", parcels.size(), dbQueryTime);
            
            if (parcels.isEmpty()) {
                log.debug("[parcel-service] [ParcelService.fetchParcelsBulk] No parcels found for provided IDs");
                return result;
            }
            
            // Step 2: Fetch all destinations in batch (1 query instead of N)
            log.debug("[parcel-service] [ParcelService.fetchParcelsBulk] Step 2: Fetching destinations for {} parcels...", parcels.size());
            List<ParcelDestination> destinations = parcelDestinationRepository.findByParcelInAndIsCurrentTrue(parcels);
            long destinationsTime = System.currentTimeMillis() - startTime - dbQueryTime;
            log.debug("[parcel-service] [ParcelService.fetchParcelsBulk] Fetched {} destinations in {}ms", destinations.size(), destinationsTime);
            
            // Step 3: Create map of parcel -> destination for quick lookup
            Map<String, ParcelDestination> parcelToDestination = destinations.stream()
                .collect(Collectors.toMap(
                    dest -> dest.getParcel().getId().toString(),
                    dest -> dest,
                    (existing, replacement) -> existing // Keep first if duplicate
                ));
            
            // Step 4: Fetch zone destinations in parallel (N parallel calls instead of sequential)
            log.debug("[parcel-service] [ParcelService.fetchParcelsBulk] Step 3: Fetching zone destinations for {} addresses...", destinations.size());
            List<String> destinationIds = destinations.stream()
                .map(ParcelDestination::getDestinationId)
                .distinct()
                .collect(Collectors.toList());
            
            long zoneStartTime = System.currentTimeMillis();
            
            // Create parallel futures for zone service calls
            ExecutorService executor = Executors.newFixedThreadPool(Math.min(10, destinationIds.size()));
            Map<String, CompletableFuture<DestinationResponse<DesDetail>>> zoneFutures = new HashMap<>();
            
            for (String destinationId : destinationIds) {
                CompletableFuture<DestinationResponse<DesDetail>> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return zoneClient.getDestination(destinationId);
                    } catch (Exception e) {
                        log.error("[parcel-service] [ParcelService.fetchParcelsBulk] Failed to fetch zone destination {}", destinationId, e);
                        return null;
                    }
                }, executor);
                zoneFutures.put(destinationId, future);
            }
            
            // Wait for all zone calls to complete
            CompletableFuture.allOf(zoneFutures.values().toArray(new CompletableFuture[0])).join();
            
            // Create map of destinationId -> DestinationResponse
            Map<String, DestinationResponse<DesDetail>> destinationMap = new HashMap<>();
            for (Map.Entry<String, CompletableFuture<DestinationResponse<DesDetail>>> entry : zoneFutures.entrySet()) {
                try {
                    DestinationResponse<DesDetail> response = entry.getValue().get();
                    if (response != null && response.getResult() != null) {
                        destinationMap.put(entry.getKey(), response);
                    }
                } catch (Exception e) {
                    log.error("[parcel-service] [ParcelService.fetchParcelsBulk] Error getting zone destination {}", entry.getKey(), e);
                }
            }
            
            // Cleanup executor
            executor.shutdown();
            
            long zoneTime = System.currentTimeMillis() - zoneStartTime;
            log.debug("[parcel-service] [ParcelService.fetchParcelsBulk] Fetched {} zone destinations in {}ms (parallel)", destinationMap.size(), zoneTime);
            
            // Step 5: Build result map by combining parcel, destination, and zone data
            log.debug("[parcel-service] [ParcelService.fetchParcelsBulk] Step 4: Building response for {} parcels...", parcels.size());
            for (Parcel parcel : parcels) {
                try {
                    String parcelIdStr = parcel.getId().toString();
                    ParcelDestination destination = parcelToDestination.get(parcelIdStr);
                    
                    if (destination == null) {
                        log.debug("[parcel-service] [ParcelService.fetchParcelsBulk] No destination found for parcel {}", parcelIdStr);
                        // Return parcel without location
                        result.put(parcelIdStr, toDto(parcel));
                        continue;
                    }
                    
                    DestinationResponse<DesDetail> zoneResponse = destinationMap.get(destination.getDestinationId());
                    
                    if (zoneResponse == null || zoneResponse.getResult() == null) {
                        log.debug("[parcel-service] [ParcelService.fetchParcelsBulk] No zone data found for destination {} (parcel {})", 
                                destination.getDestinationId(), parcelIdStr);
                        // Return parcel without location
                        result.put(parcelIdStr, toDto(parcel));
                        continue;
                    }
                    
                    // Build response with location
                    ParcelResponse parcelResponse = toDtoWithLocation(parcel, zoneResponse);
                    result.put(parcelIdStr, parcelResponse);
                } catch (Exception e) {
                    log.error("[parcel-service] [ParcelService.fetchParcelsBulk] Error building response for parcel {}", parcel.getId(), e);
                    // Return parcel without location as fallback
                    result.put(parcel.getId().toString(), toDto(parcel));
                }
            }
            
            long totalTime = System.currentTimeMillis() - startTime;
            log.debug("[parcel-service] [ParcelService.fetchParcelsBulk] Completed fetchParcelsBulk: {} parcels in {}ms (DB: {}ms, Destinations: {}ms, Zone: {}ms)", 
                    result.size(), totalTime, dbQueryTime, destinationsTime, zoneTime);
            
        } catch (Exception e) {
            log.error("[parcel-service] [ParcelService.fetchParcelsBulk] Critical error in fetchParcelsBulk", e);
            // Fallback: return empty map or parcels without locations
            try {
                List<Parcel> parcels = parcelRepository.findAllById(parcelIds);
                for (Parcel parcel : parcels) {
                    result.put(parcel.getId().toString(), toDto(parcel));
                }
            } catch (Exception fallbackError) {
                log.error("[parcel-service] [ParcelService.fetchParcelsBulk] Fallback also failed", fallbackError);
            }
        }
        
        return result;
    }

    @Override
    public PageResponse<ParcelResponse> getParcelsSentByCustomer(String customerId, int page, int size) {
        Pageable pageable = PageUtil.build(page, size, "createdAt", "DESC", Parcel.class);
        
        Page<Parcel> parcels = parcelRepository.findBySenderId(customerId, pageable);
        
        return PageResponse.from(parcels.map(this::toDto));
    }

    @Override
    public PageResponse<ParcelResponse> getParcelsReceivedByCustomer(String customerId, int page, int size) {
        Pageable pageable = PageUtil.build(page, size, "createdAt", "DESC", Parcel.class);
        
        Page<Parcel> parcels = parcelRepository.findByReceiverId(customerId, pageable);
        
        return PageResponse.from(parcels.map(this::toDto));
    }

    @Override
    @Transactional
    public ParcelResponse updateParcelPriority(UUID parcelId, Integer priority) {
        Parcel parcel = parcelRepository.findById(parcelId)
            .orElseThrow(() -> new ResourceNotFound("Parcel not found with id: " + parcelId));
        
        log.debug("[parcel-service] [ParcelService.updatePriority] Updating priority for parcel {} from {} to {}", parcelId, parcel.getPriority(), priority);
        parcel.setPriority(priority);
        
        Parcel updatedParcel = parcelRepository.save(parcel);
        return toDto(updatedParcel);
    }

    @Override
    @Transactional
    public ParcelResponse delayParcel(UUID parcelId, LocalDateTime delayedUntil) {
        Parcel parcel = parcelRepository.findById(parcelId)
            .orElseThrow(() -> new ResourceNotFound("Parcel not found with id: " + parcelId));
        
        log.debug("[parcel-service] [ParcelService.delayParcel] Delaying parcel {} until {}", parcelId, delayedUntil);
        parcel.setIsDelayed(true);
        parcel.setDelayedUntil(delayedUntil);
        
        Parcel updatedParcel = parcelRepository.save(parcel);
        return toDto(updatedParcel);
    }

    @Override
    @Transactional
    public ParcelResponse undelayParcel(UUID parcelId) {
        Parcel parcel = parcelRepository.findById(parcelId)
            .orElseThrow(() -> new ResourceNotFound("Parcel not found with id: " + parcelId));
        
        log.debug("[parcel-service] [ParcelService.undelayParcel] Undelaying parcel {}", parcelId);
        parcel.setIsDelayed(false);
        parcel.setDelayedUntil(null);
        
        Parcel updatedParcel = parcelRepository.save(parcel);
        return toDto(updatedParcel);
    }

    //update address
    // parcel staticstic
}
