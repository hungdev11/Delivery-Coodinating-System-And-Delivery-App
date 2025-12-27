package com.ds.parcel_service.business.v1.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.ds.parcel_service.application.client.DesDetail;
import com.ds.parcel_service.application.client.DestinationResponse;
import com.ds.parcel_service.application.client.ZoneClient;
import com.ds.parcel_service.application.client.ZoneInfo;
import com.ds.parcel_service.application.client.ZoneInfoFromAddress;
import com.ds.parcel_service.application.client.SessionServiceClient;
import com.ds.parcel_service.application.services.AssignmentService;
import com.ds.parcel_service.infrastructure.kafka.dto.SeedProgressEvent;
import com.ds.parcel_service.infrastructure.kafka.KafkaConfig;
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
            case IN_WAREHOUSE -> next == ParcelStatus.ON_ROUTE || next == ParcelStatus.DISPUTE; // Allow admin to set dispute
            case ON_ROUTE -> next == ParcelStatus.DELIVERED || next == ParcelStatus.FAILED || next == ParcelStatus.DELAYED || next == ParcelStatus.DISPUTE; // Allow admin to set dispute
            case DELIVERED -> next == ParcelStatus.SUCCEEDED || next == ParcelStatus.FAILED || next == ParcelStatus.DISPUTE;
            case DISPUTE -> next == ParcelStatus.SUCCEEDED || next == ParcelStatus.LOST;
            case DELAYED -> next == ParcelStatus.IN_WAREHOUSE || next == ParcelStatus.DISPUTE; // Allow admin to set dispute
            case FAILED -> next == ParcelStatus.IN_WAREHOUSE; 
            case SUCCEEDED, LOST -> false; // Last state
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

        // mark as it failed when moving back to IN_WAREHOUSE from FAILED distinguishes from normal return
        if (currentStatus == ParcelStatus.FAILED && nextStatus == ParcelStatus.IN_WAREHOUSE) {
            parcel.setIsFail(true);
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
        Parcel parcel = processTransition(parcelId, event);
        return toDtoWithFullObjectsForSingle(parcel);
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

        return toDtoWithFullObjectsForSingle(saved);
    }

    @Override
    @Transactional
    public ParcelResponse createParcel(ParcelCreateRequest request) {
        validateUniqueCode(request.getCode());
        
        // Validate address IDs are provided
        if (request.getSenderAddressId() == null || request.getSenderAddressId().isBlank()) {
            throw new IllegalArgumentException("Sender address ID is required");
        }
        if (request.getReceiverAddressId() == null || request.getReceiverAddressId().isBlank()) {
            throw new IllegalArgumentException("Receiver address ID is required");
        }
        
        // Fetch UserAddress to get destinationId for ParcelDestination
        UserServiceClient.UserAddressInfo senderAddress = userServiceClient.getUserAddressById(request.getSenderAddressId());
        if (senderAddress == null) {
            throw new IllegalArgumentException("Sender address not found: " + request.getSenderAddressId());
        }
        
        UserServiceClient.UserAddressInfo receiverAddress = userServiceClient.getUserAddressById(request.getReceiverAddressId());
        if (receiverAddress == null) {
            throw new IllegalArgumentException("Receiver address not found: " + request.getReceiverAddressId());
        }
        
        Parcel parcel = Parcel.builder()
                            .code(request.getCode())
                            .deliveryType(DeliveryType.valueOf(request.getDeliveryType()))
                            .senderId(request.getSenderId())
                            .receiverId(request.getReceiverId())
                            .senderAddressId(request.getSenderAddressId())
                            .receiverAddressId(request.getReceiverAddressId())
                            .status(ParcelStatus.IN_WAREHOUSE) 
                            .weight(request.getWeight())
                            .value(request.getValue())
                            .windowStart(request.getWindowStart())
                            .windowEnd(request.getWindowEnd())
                            .build();
        
        Parcel savedParcel = parcelRepository.save(parcel);

        // Link parcel to receiver destination (PRIMARY) - this is the current destination
        ParcelDestination receiverPd = ParcelDestination.builder()
            .destinationId(receiverAddress.getDestinationId())
            .destinationType(DestinationType.PRIMARY)
            .isCurrent(true)
            .isOriginal(true)
            .parcel(savedParcel)
            .build();

        parcelDestinationRepository.save(receiverPd);
        log.debug("[parcel-service] [ParcelService.createParcel] Linked parcel {} to receiver destination {} (PRIMARY, current)", savedParcel.getId(), receiverAddress.getDestinationId());

        // Link parcel to sender destination (SECONDARY) - not current
        ParcelDestination senderPd = ParcelDestination.builder()
            .destinationId(senderAddress.getDestinationId())
            .destinationType(DestinationType.SECONDARY)
            .isCurrent(false)
            .isOriginal(true)
            .parcel(savedParcel)
            .build();

        parcelDestinationRepository.save(senderPd);
        log.debug("[parcel-service] [ParcelService.createParcel] Linked parcel {} to sender destination {} (SECONDARY)", savedParcel.getId(), senderAddress.getDestinationId());
        
        // Validate: Ensure only one destination is current
        validateSingleCurrentDestination(savedParcel);
        
        return toDtoWithFullObjectsForSingle(savedParcel);
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
        Parcel savedParcel = parcelRepository.save(parcel);
        return toDtoWithFullObjectsForSingle(savedParcel);
    }

    @Override
    public void deleteParcel(UUID parcelId) {
        // [Logic: Thêm kiểm tra trạng thái trước khi cho phép xóa]
        return;
    }

    @Override
    public ParcelResponse getParcelById(UUID parcelId) {
        Parcel parcel = getParcel(parcelId);
        return toDtoWithFullObjectsForSingle(parcel);
    }

    @Override
    public ParcelResponse getParcelByCode(String code) {
        Parcel parcel = parcelRepository.findByCode(code).orElseThrow(()-> new ResourceNotFound("Not found parcel with code " + code));
        return toDtoWithFullObjectsForSingle(parcel);
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
        return toPageResponseWithBulkUserFetch(parcels);
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
        return toPageResponseWithBulkUserFetch(parcels);
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
        
        // Use bulk fetch helper which fetches users, addresses, and destinations
        return toPageResponseWithBulkUserFetch(parcels);
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
        PageResponse<ParcelResponse> pageResponse = toPageResponseWithBulkUserFetch(parcels);
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

    /**
     * Convert list of Parcels to DTOs with bulk user info and address fetching (optimized)
     */
    private List<ParcelResponse> toDtoListWithBulkUserFetch(List<Parcel> parcels) {
        if (parcels == null || parcels.isEmpty()) {
            return List.of();
        }
        
        // Step 1: Collect all unique user IDs
        List<String> userIds = parcels.stream()
            .flatMap(p -> java.util.stream.Stream.of(p.getSenderId(), p.getReceiverId()))
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .collect(java.util.stream.Collectors.toList());
        
        // Step 2: Fetch all users in bulk
        Map<String, UserServiceClient.UserInfo> usersMap = userIds.isEmpty() 
            ? new java.util.HashMap<>() 
            : userServiceClient.getUsersByIds(userIds);
        
        // Step 3: Collect all unique address IDs
        List<String> addressIds = parcels.stream()
            .flatMap(p -> java.util.stream.Stream.of(p.getSenderAddressId(), p.getReceiverAddressId()))
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .collect(java.util.stream.Collectors.toList());
        
        // Step 4: Fetch all addresses in parallel (UserAddress from user-service)
        Map<String, UserServiceClient.UserAddressInfo> addressesMap = fetchAddressesBulk(addressIds);
        
        // Step 5: Collect all destination IDs from addresses and fetch from zone-service
        List<String> destinationIds = addressesMap.values().stream()
            .map(UserServiceClient.UserAddressInfo::getDestinationId)
            .filter(id -> id != null && !id.isBlank())
            .distinct()
            .collect(java.util.stream.Collectors.toList());
        
        Map<String, DesDetail> destinationsMap = fetchDestinationsBulk(destinationIds);
        
        // Step 6: No need to fetch zones separately - zone and center info is already in DesDetail from zone-service
        // The nested zone object in DesDetail already contains all necessary info
        
        // Step 7: Convert to DTOs with full nested objects (including zone.center from DesDetail)
        return parcels.stream()
            .map(p -> toDtoWithFullObjects(p, usersMap, addressesMap, destinationsMap))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Convert Page of Parcels to PageResponse with bulk user info fetching (optimized)
     */
    private PageResponse<ParcelResponse> toPageResponseWithBulkUserFetch(Page<Parcel> parcels) {
        List<ParcelResponse> content = toDtoListWithBulkUserFetch(parcels.getContent());
        return new PageResponse<>(
            content,
            parcels.getNumber(),
            parcels.getSize(),
            parcels.getTotalElements(),
            parcels.getTotalPages(),
            parcels.isFirst(),
            parcels.isLast()
        );
    }
    
    /**
     * Convert single Parcel to DTO with full nested objects (for single parcel operations)
     */
    private ParcelResponse toDtoWithFullObjectsForSingle(Parcel parcel) {
        List<ParcelResponse> results = toDtoListWithBulkUserFetch(List.of(parcel));
        return results.isEmpty() ? toDto(parcel) : results.get(0);
    }

    /**
     * Convert Parcel to DTO with full nested objects (users, addresses with coordinates, zones with centers)
     */
    private ParcelResponse toDtoWithFullObjects(
            Parcel parcel,
            Map<String, UserServiceClient.UserInfo> usersMap,
            Map<String, UserServiceClient.UserAddressInfo> addressesMap,
            Map<String, DesDetail> destinationsMap) {
        
        // Build sender user object
        ParcelResponse.UserInfoDto sender = null;
        String senderName = null;
        if (parcel.getSenderId() != null && usersMap.containsKey(parcel.getSenderId())) {
            UserServiceClient.UserInfo senderInfo = usersMap.get(parcel.getSenderId());
            if (senderInfo != null) {
                senderName = senderInfo.getFullName();
                sender = ParcelResponse.UserInfoDto.builder()
                    .id(senderInfo.getId())
                    .firstName(senderInfo.getFirstName())
                    .lastName(senderInfo.getLastName())
                    .username(senderInfo.getUsername())
                    .email(senderInfo.getEmail())
                    .phone(senderInfo.getPhone())
                    .address(senderInfo.getAddress())
                    .build();
            }
        }
        
        // Build receiver user object
        ParcelResponse.UserInfoDto receiver = null;
        String receiverName = null;
        String receiverPhoneNumber = null;
        if (parcel.getReceiverId() != null && usersMap.containsKey(parcel.getReceiverId())) {
            UserServiceClient.UserInfo receiverInfo = usersMap.get(parcel.getReceiverId());
            if (receiverInfo != null) {
                receiverName = receiverInfo.getFullName();
                receiverPhoneNumber = receiverInfo.getPhone();
                receiver = ParcelResponse.UserInfoDto.builder()
                    .id(receiverInfo.getId())
                    .firstName(receiverInfo.getFirstName())
                    .lastName(receiverInfo.getLastName())
                    .username(receiverInfo.getUsername())
                    .email(receiverInfo.getEmail())
                    .phone(receiverInfo.getPhone())
                    .address(receiverInfo.getAddress())
                    .build();
            }
        }
        
        // Build sender address object with zone.center
        ParcelResponse.AddressInfoDto senderAddress = null;
        if (parcel.getSenderAddressId() != null && !parcel.getSenderAddressId().isBlank()) {
            UserServiceClient.UserAddressInfo userAddr = addressesMap.get(parcel.getSenderAddressId());
            if (userAddr != null) {
                DesDetail desDetail = destinationsMap.get(userAddr.getDestinationId());
                // Build ZoneInfoDto directly from nested zone object in DesDetail
                ParcelResponse.ZoneInfoDto zoneDto = buildZoneInfoDtoFromAddress(desDetail);
                
                senderAddress = ParcelResponse.AddressInfoDto.builder()
                    .id(userAddr.getId())
                    .userId(userAddr.getUserId())
                    .destinationId(userAddr.getDestinationId())
                    .note(userAddr.getNote())
                    .tag(userAddr.getTag())
                    .isPrimary(userAddr.getIsPrimary())
                    .lat(desDetail != null ? desDetail.getLat() : null)
                    .lon(desDetail != null ? desDetail.getLon() : null)
                    .zoneId(desDetail != null 
                        ? (desDetail.getZone() != null ? desDetail.getZone().getId() : desDetail.getZoneId())
                        : null)
                    .zone(zoneDto)
                    .build();
            }
        }
        
        // Build receiver address object with zone.center
        ParcelResponse.AddressInfoDto receiverAddress = null;
        if (parcel.getReceiverAddressId() != null && !parcel.getReceiverAddressId().isBlank()) {
            UserServiceClient.UserAddressInfo userAddr = addressesMap.get(parcel.getReceiverAddressId());
            if (userAddr != null) {
                DesDetail desDetail = destinationsMap.get(userAddr.getDestinationId());
                // Build ZoneInfoDto directly from nested zone object in DesDetail
                ParcelResponse.ZoneInfoDto zoneDto = buildZoneInfoDtoFromAddress(desDetail);
                
                receiverAddress = ParcelResponse.AddressInfoDto.builder()
                    .id(userAddr.getId())
                    .userId(userAddr.getUserId())
                    .destinationId(userAddr.getDestinationId())
                    .note(userAddr.getNote())
                    .tag(userAddr.getTag())
                    .isPrimary(userAddr.getIsPrimary())
                    .lat(desDetail != null ? desDetail.getLat() : null)
                    .lon(desDetail != null ? desDetail.getLon() : null)
                    .zoneId(desDetail != null 
                        ? (desDetail.getZone() != null ? desDetail.getZone().getId() : desDetail.getZoneId())
                        : null)
                    .zone(zoneDto)
                    .build();
            }
        }
        
        // Build response with full objects
        return buildParcelResponse(parcel, senderName, receiverName, receiverPhoneNumber, sender, receiver, senderAddress, receiverAddress);
    }
    
    /**
     * Build ZoneInfoDto from nested zone object in DesDetail (from zone-service address response)
     */
    private ParcelResponse.ZoneInfoDto buildZoneInfoDtoFromAddress(DesDetail desDetail) {
        if (desDetail == null) {
            log.debug("[parcel-service] [ParcelService.buildZoneInfoDtoFromAddress] desDetail is null");
            return null;
        }
        
        if (desDetail.getZone() == null) {
            log.debug("[parcel-service] [ParcelService.buildZoneInfoDtoFromAddress] desDetail.zone is null, zoneId: {}", desDetail.getZoneId());
            return null;
        }
        
        ZoneInfoFromAddress zoneFromAddress = desDetail.getZone();
        log.debug("[parcel-service] [ParcelService.buildZoneInfoDtoFromAddress] Building zone DTO for zoneId: {}, zoneCode: {}, zoneName: {}", 
            zoneFromAddress.getId(), zoneFromAddress.getCode(), zoneFromAddress.getName());
        
        ParcelResponse.CenterInfoDto centerDto = null;
        if (zoneFromAddress.getCenter() != null) {
            ZoneInfoFromAddress.CenterInfoFromAddress centerFromAddress = zoneFromAddress.getCenter();
            centerDto = ParcelResponse.CenterInfoDto.builder()
                .id(centerFromAddress.getId())
                .code(centerFromAddress.getCode())
                .name(centerFromAddress.getName())
                .address(centerFromAddress.getAddress())
                .lat(centerFromAddress.getLat())
                .lon(centerFromAddress.getLon())
                .build();
        }
        
        return ParcelResponse.ZoneInfoDto.builder()
            .id(zoneFromAddress.getId())
            .code(zoneFromAddress.getCode())
            .name(zoneFromAddress.getName())
            .center(centerDto)
            .build();
    }
    
    /**
     * Build ZoneInfoDto from ZoneInfo, including center information
     * @deprecated Use buildZoneInfoDtoFromAddress instead
     */
    @Deprecated
    private ParcelResponse.ZoneInfoDto buildZoneInfoDto(ZoneInfo zoneInfo) {
        if (zoneInfo == null) {
            return null;
        }
        
        ParcelResponse.CenterInfoDto centerDto = null;
        if (zoneInfo.getCenterId() != null) {
            centerDto = ParcelResponse.CenterInfoDto.builder()
                .id(zoneInfo.getCenterId())
                .code(zoneInfo.getCenterCode())
                .name(zoneInfo.getCenterName())
                .address(zoneInfo.getCenterAddress())
                .lat(zoneInfo.getCenterLat())
                .lon(zoneInfo.getCenterLon())
                .build();
        }
        
        return ParcelResponse.ZoneInfoDto.builder()
            .id(zoneInfo.getId())
            .code(zoneInfo.getCode())
            .name(zoneInfo.getName())
            .center(centerDto)
            .build();
    }
    
    /**
     * Convert Parcel to DTO with user info from pre-fetched map (optimized for bulk operations)
     * @deprecated Use toDtoWithFullObjects for full nested objects
     */
    private ParcelResponse toDtoWithUserInfo(Parcel parcel, Map<String, UserServiceClient.UserInfo> usersMap) {
        String senderName = null;
        String receiverName = null;
        String receiverPhoneNumber = null;
        
        if (parcel.getSenderId() != null && usersMap.containsKey(parcel.getSenderId())) {
            UserServiceClient.UserInfo senderInfo = usersMap.get(parcel.getSenderId());
            if (senderInfo != null) {
                senderName = senderInfo.getFullName();
            }
        }
        if (parcel.getReceiverId() != null && usersMap.containsKey(parcel.getReceiverId())) {
            UserServiceClient.UserInfo receiverInfo = usersMap.get(parcel.getReceiverId());
            if (receiverInfo != null) {
                receiverName = receiverInfo.getFullName();
                receiverPhoneNumber = receiverInfo.getPhone();
            }
        }
        
        return buildParcelResponse(parcel, senderName, receiverName, receiverPhoneNumber);
    }

    /**
     * Convert Parcel to DTO (legacy method - calls User Service for each parcel)
     * @deprecated Use toDtoWithUserInfo for bulk operations
     */
    private ParcelResponse toDto(Parcel parcel) {
        // Get sender and receiver info from User Service (one-by-one - not optimized)
        String senderName = null;
        String receiverName = null;
        String receiverPhoneNumber = null;
        
        try {
            if (parcel.getSenderId() != null) {
                UserServiceClient.UserInfo senderInfo = userServiceClient.getUserById(parcel.getSenderId());
                if (senderInfo != null) {
                    senderName = senderInfo.getFullName();
                }
            }
            if (parcel.getReceiverId() != null) {
                UserServiceClient.UserInfo receiverInfo = userServiceClient.getUserById(parcel.getReceiverId());
                if (receiverInfo != null) {
                    receiverName = receiverInfo.getFullName();
                    receiverPhoneNumber = receiverInfo.getPhone();
                }
            }
        } catch (Exception e) {
            log.debug("[parcel-service] [ParcelService.toDto] Could not fetch user info for parcel {}: {}", parcel.getId(), e.getMessage());
        }
        
        return buildParcelResponse(parcel, senderName, receiverName, receiverPhoneNumber);
    }

    /**
     * Build ParcelResponse from Parcel entity with full nested objects
     */
    private ParcelResponse buildParcelResponse(
            Parcel parcel,
            String senderName,
            String receiverName,
            String receiverPhoneNumber,
            ParcelResponse.UserInfoDto sender,
            ParcelResponse.UserInfoDto receiver,
            ParcelResponse.AddressInfoDto senderAddress,
            ParcelResponse.AddressInfoDto receiverAddress) {
        return ParcelResponse.builder()
                            .id(parcel.getId().toString())
                            .code(parcel.getCode())
                            .deliveryType(parcel.getDeliveryType())
                            .senderId(parcel.getSenderId())
                            .senderName(senderName)
                            .receiverId(parcel.getReceiverId())
                            .receiverName(receiverName)
                            .receiverPhoneNumber(receiverPhoneNumber)
                            .senderAddressId(parcel.getSenderAddressId())
                            .receiverAddressId(parcel.getReceiverAddressId())
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
                            .sender(sender)
                            .receiver(receiver)
                            .senderAddress(senderAddress)
                            .receiverAddress(receiverAddress)
                            .build();
    }
    
    /**
     * Build ParcelResponse from Parcel entity and user info (legacy method without nested objects)
     */
    private ParcelResponse buildParcelResponse(Parcel parcel, String senderName, String receiverName, String receiverPhoneNumber) {
        return buildParcelResponse(parcel, senderName, receiverName, receiverPhoneNumber, null, null, null, null);
    }
    
    /**
     * Bulk fetch addresses from User Service in parallel
     */
    private Map<String, UserServiceClient.UserAddressInfo> fetchAddressesBulk(List<String> addressIds) {
        if (addressIds == null || addressIds.isEmpty()) {
            return new HashMap<>();
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(10, addressIds.size()));
        Map<String, CompletableFuture<UserServiceClient.UserAddressInfo>> futures = new HashMap<>();
        
        for (String addressId : addressIds) {
            CompletableFuture<UserServiceClient.UserAddressInfo> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return userServiceClient.getUserAddressById(addressId);
                } catch (Exception e) {
                    log.debug("[parcel-service] [ParcelService.fetchAddressesBulk] Failed to fetch address {}: {}", addressId, e.getMessage());
                    return null;
                }
            }, executor);
            futures.put(addressId, future);
        }
        
        CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();
        
        Map<String, UserServiceClient.UserAddressInfo> result = new HashMap<>();
        for (Map.Entry<String, CompletableFuture<UserServiceClient.UserAddressInfo>> entry : futures.entrySet()) {
            try {
                UserServiceClient.UserAddressInfo address = entry.getValue().get();
                if (address != null) {
                    result.put(entry.getKey(), address);
                }
            } catch (Exception e) {
                log.debug("[parcel-service] [ParcelService.fetchAddressesBulk] Error getting address {}: {}", entry.getKey(), e.getMessage());
            }
        }
        
        executor.shutdown();
        return result;
    }
    
    /**
     * Bulk fetch destinations from Zone Service in parallel
     */
    private Map<String, DesDetail> fetchDestinationsBulk(List<String> destinationIds) {
        if (destinationIds == null || destinationIds.isEmpty()) {
            return new HashMap<>();
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(10, destinationIds.size()));
        Map<String, CompletableFuture<DestinationResponse<DesDetail>>> futures = new HashMap<>();
        
        for (String destinationId : destinationIds) {
            CompletableFuture<DestinationResponse<DesDetail>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return zoneClient.getDestination(destinationId);
                } catch (Exception e) {
                    log.debug("[parcel-service] [ParcelService.fetchDestinationsBulk] Failed to fetch destination {}: {}", destinationId, e.getMessage());
                    return null;
                }
            }, executor);
            futures.put(destinationId, future);
        }
        
        CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();
        
        Map<String, DesDetail> result = new HashMap<>();
        for (Map.Entry<String, CompletableFuture<DestinationResponse<DesDetail>>> entry : futures.entrySet()) {
            try {
                DestinationResponse<DesDetail> response = entry.getValue().get();
                if (response != null && response.getResult() != null) {
                    result.put(entry.getKey(), response.getResult());
                }
            } catch (Exception e) {
                log.debug("[parcel-service] [ParcelService.fetchDestinationsBulk] Error getting destination {}: {}", entry.getKey(), e.getMessage());
            }
        }
        
        executor.shutdown();
        return result;
    }
    
    /**
     * Bulk fetch zones from Zone Service using V2 API with filter
     */
    private Map<String, ZoneInfo> fetchZonesBulk(List<String> zoneIds) {
        if (zoneIds == null || zoneIds.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            // Build V2 filter: id IN (zoneIds)
            Map<String, Object> filter = new HashMap<>();
            filter.put("type", "condition");
            filter.put("field", "id");
            filter.put("operator", "IN");
            filter.put("value", zoneIds);
            
            Map<String, Object> filters = new HashMap<>();
            filters.put("type", "group");
            filters.put("operator", "AND");
            filters.put("items", List.of(filter));
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("filters", filters);
            requestBody.put("page", 0);
            requestBody.put("size", zoneIds.size()); // Request all at once
            
            log.debug("[parcel-service] [ParcelService.fetchZonesBulk] Calling Zone Service V2 bulk API for {} zones", zoneIds.size());
            
            com.ds.parcel_service.common.entities.dto.common.BaseResponse<com.ds.parcel_service.common.entities.dto.common.PagedData<Map<String, Object>>> response = 
                zoneClient.getZonesV2(requestBody);
            
            if (response != null && response.getResult() != null && response.getResult().getData() != null) {
                Map<String, ZoneInfo> result = new HashMap<>();
                for (Map<String, Object> zoneMap : response.getResult().getData()) {
                    ZoneInfo zoneInfo = new ZoneInfo();
                    zoneInfo.setId((String) zoneMap.get("id"));
                    zoneInfo.setCode((String) zoneMap.get("code"));
                    zoneInfo.setName((String) zoneMap.get("name"));
                    zoneInfo.setCenterId((String) zoneMap.get("centerId"));
                    zoneInfo.setCenterCode((String) zoneMap.get("centerCode"));
                    zoneInfo.setCenterName((String) zoneMap.get("centerName"));
                    zoneInfo.setCenterAddress((String) zoneMap.get("centerAddress"));
                    
                    // Map centerLat and centerLon (might be Double or BigDecimal)
                    Object centerLatObj = zoneMap.get("centerLat");
                    if (centerLatObj != null) {
                        if (centerLatObj instanceof Number) {
                            zoneInfo.setCenterLat(BigDecimal.valueOf(((Number) centerLatObj).doubleValue()));
                        }
                    }
                    Object centerLonObj = zoneMap.get("centerLon");
                    if (centerLonObj != null) {
                        if (centerLonObj instanceof Number) {
                            zoneInfo.setCenterLon(BigDecimal.valueOf(((Number) centerLonObj).doubleValue()));
                        }
                    }
                    
                    if (zoneInfo.getId() != null) {
                        result.put(zoneInfo.getId(), zoneInfo);
                    }
                }
                log.debug("[parcel-service] [ParcelService.fetchZonesBulk] Retrieved {} zones from Zone Service", result.size());
                return result;
            }
            
            log.debug("[parcel-service] [ParcelService.fetchZonesBulk] No zones found in Zone Service");
            return new HashMap<>();
        } catch (Exception e) {
            log.error("[parcel-service] [ParcelService.fetchZonesBulk] Error fetching zones: {}", e.getMessage(), e);
            return new HashMap<>();
        }
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
        
        return toPageResponseWithBulkUserFetch(parcels);
    }

    @Override
    public PageResponse<ParcelResponse> getParcelsReceivedByCustomer(String customerId, int page, int size) {
        Pageable pageable = PageUtil.build(page, size, "createdAt", "DESC", Parcel.class);
        
        Page<Parcel> parcels = parcelRepository.findByReceiverId(customerId, pageable);
        
        return toPageResponseWithBulkUserFetch(parcels);
    }

    @Override
    @Transactional
    public ParcelResponse updateParcelPriority(UUID parcelId, Integer priority) {
        Parcel parcel = parcelRepository.findById(parcelId)
            .orElseThrow(() -> new ResourceNotFound("Parcel not found with id: " + parcelId));
        
        log.debug("[parcel-service] [ParcelService.updatePriority] Updating priority for parcel {} from {} to {}", parcelId, parcel.getPriority(), priority);
        parcel.setPriority(priority);
        
        Parcel updatedParcel = parcelRepository.save(parcel);
        return toDtoWithFullObjectsForSingle(updatedParcel);
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
        return toDtoWithFullObjectsForSingle(updatedParcel);
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
        return toDtoWithFullObjectsForSingle(updatedParcel);
    }

    @Override
    public List<ParcelResponse> getParcelsByIds(List<UUID> parcelIds) {
        if (parcelIds == null || parcelIds.isEmpty()) {
            return List.of();
        }
        log.debug("Getting parcels by IDs: count={}", parcelIds.size());
        List<Parcel> parcels = parcelRepository.findByIdIn(parcelIds);
        return toDtoListWithBulkUserFetch(parcels);
    }

    @Override
    public List<ParcelResponse> getParcelsBySenderIds(List<String> senderIds) {
        if (senderIds == null || senderIds.isEmpty()) {
            return List.of();
        }
        log.debug("Getting parcels by sender IDs: count={}", senderIds.size());
        List<Parcel> parcels = parcelRepository.findBySenderIdIn(senderIds);
        return toDtoListWithBulkUserFetch(parcels);
    }

    @Override
    public List<ParcelResponse> getParcelsByReceiverIds(List<String> receiverIds) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            return List.of();
        }
        log.debug("Getting parcels by receiver IDs: count={}", receiverIds.size());
        List<Parcel> parcels = parcelRepository.findByReceiverIdIn(receiverIds);
        return toDtoListWithBulkUserFetch(parcels);
    }

    @Override
    public PageResponse<ParcelResponse> getParcelsByIdsPaged(List<UUID> parcelIds, int page, int size, String sortBy, String direction) {
        if (parcelIds == null || parcelIds.isEmpty()) {
            // Create empty PageResponse manually
            int totalPages = 0;
            return new PageResponse<>(
                List.of(),
                page,
                size,
                0L,
                totalPages,
                true,
                true
            );
        }
        log.debug("Getting parcels by IDs with paging: count={}, page={}, size={}", parcelIds.size(), page, size);
        
        // Fetch all matching parcels
        List<Parcel> allParcels = parcelRepository.findByIdIn(parcelIds);
        
        // Apply sorting if provided
        if (sortBy != null && !sortBy.isEmpty()) {
            Pageable pageable = PageUtil.build(page, size, sortBy, direction, Parcel.class);
            // Note: For bulk queries, we apply sorting manually after fetching
            // This is less efficient but necessary for bulk queries
            allParcels = allParcels.stream()
                    .sorted((p1, p2) -> {
                        // Simple sorting - can be enhanced
                        return p1.getCreatedAt().compareTo(p2.getCreatedAt());
                    })
                    .collect(Collectors.toList());
        }
        
        // Apply pagination
        int start = page * size;
        int end = Math.min(start + size, allParcels.size());
        List<Parcel> pagedParcels = start < allParcels.size() ? allParcels.subList(start, end) : List.of();
        
        // Convert to DTOs with join support (fetch user info in bulk)
        List<ParcelResponse> content = toDtoListWithBulkUserFetch(pagedParcels);
        
        // Calculate pagination metadata
        long totalElements = allParcels.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean isFirst = page == 0;
        boolean isLast = (page + 1) >= totalPages || content.isEmpty();
        
        return new PageResponse<>(
            content,
            page,
            size,
            totalElements,
            totalPages,
            isFirst,
            isLast
        );
    }

    @Override
    public PageResponse<ParcelResponse> getParcelsBySenderIdsPaged(List<String> senderIds, int page, int size, String sortBy, String direction) {
        if (senderIds == null || senderIds.isEmpty()) {
            // Create empty PageResponse manually
            int totalPages = 0;
            return new PageResponse<>(
                List.of(),
                page,
                size,
                0L,
                totalPages,
                true,
                true
            );
        }
        log.debug("Getting parcels by sender IDs with paging: count={}, page={}, size={}", senderIds.size(), page, size);
        
        List<Parcel> allParcels = parcelRepository.findBySenderIdIn(senderIds);
        
        // Apply sorting if provided
        if (sortBy != null && !sortBy.isEmpty()) {
            allParcels = allParcels.stream()
                    .sorted((p1, p2) -> p1.getCreatedAt().compareTo(p2.getCreatedAt()))
                    .collect(Collectors.toList());
        }
        
        int start = page * size;
        int end = Math.min(start + size, allParcels.size());
        List<Parcel> pagedParcels = start < allParcels.size() ? allParcels.subList(start, end) : List.of();
        
        List<ParcelResponse> content = toDtoListWithBulkUserFetch(pagedParcels);
        
        // Calculate pagination metadata
        long totalElements = allParcels.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean isFirst = page == 0;
        boolean isLast = (page + 1) >= totalPages || content.isEmpty();
        
        return new PageResponse<>(
            content,
            page,
            size,
            totalElements,
            totalPages,
            isFirst,
            isLast
        );
    }

    @Override
    public PageResponse<ParcelResponse> getParcelsByReceiverIdsPaged(List<String> receiverIds, int page, int size, String sortBy, String direction) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            // Create empty PageResponse manually
            int totalPages = 0;
            return new PageResponse<>(
                List.of(),
                page,
                size,
                0L,
                totalPages,
                true,
                true
            );
        }
        log.debug("Getting parcels by receiver IDs with paging: count={}, page={}, size={}", receiverIds.size(), page, size);
        
        List<Parcel> allParcels = parcelRepository.findByReceiverIdIn(receiverIds);
        
        // Apply sorting if provided
        if (sortBy != null && !sortBy.isEmpty()) {
            allParcels = allParcels.stream()
                    .sorted((p1, p2) -> p1.getCreatedAt().compareTo(p2.getCreatedAt()))
                    .collect(Collectors.toList());
        }
        
        int start = page * size;
        int end = Math.min(start + size, allParcels.size());
        List<Parcel> pagedParcels = start < allParcels.size() ? allParcels.subList(start, end) : List.of();
        
        List<ParcelResponse> content = toDtoListWithBulkUserFetch(pagedParcels);
        
        // Calculate pagination metadata
        long totalElements = allParcels.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean isFirst = page == 0;
        boolean isLast = (page + 1) >= totalPages || content.isEmpty();
        
        return new PageResponse<>(
            content,
            page,
            size,
            totalElements,
            totalPages,
            isFirst,
            isLast
        );
    }

    //update address
    // parcel staticstic
    
    /**
     * Auto seed parcels:
     * 1. Fail parcels older than 48 hours with status DELAYED, IN_WAREHOUSE, ON_ROUTE
     * 2. For each client, check addresses that don't have parcels in DELAYED, IN_WAREHOUSE, ON_ROUTE status
     * 3. Seed one parcel per address (rule: max 1 parcel per user/address in those statuses)
     * 
     * @param sessionKey Optional session key for progress tracking. If provided, will emit progress events via Kafka
     */
    @Transactional
    public AutoSeedResult autoSeedParcels(String sessionKey) {
        log.info("[parcel-service] [ParcelService.autoSeedParcels] Starting auto seed parcels, sessionKey: {}", sessionKey);
        int failedOldParcelsCount = 0;
        int seededParcelsCount = 0;
        int skippedAddressesCount = 0;
        
        try {
            // Emit STARTED event if sessionKey provided
            if (sessionKey != null && !sessionKey.isBlank()) {
                publishSeedProgressEvent(sessionKey, SeedProgressEvent.EventType.STARTED, 1, 5, 
                    "Starting auto seed process", 0, 0, 0, 0, null, null, null);
            }
            // Step 1: Fail parcels older than 48 hours
            LocalDateTime fortyEightHoursAgo = LocalDateTime.now().minusHours(48);
            List<ParcelStatus> statusesToFail = List.of(
                ParcelStatus.DELAYED,
                ParcelStatus.IN_WAREHOUSE,
                ParcelStatus.ON_ROUTE
            );
            
            List<Parcel> oldParcels = parcelRepository.findByStatusInAndCreatedAtBefore(
                statusesToFail,
                fortyEightHoursAgo
            );
            
            log.info("[parcel-service] [ParcelService.autoSeedParcels] Found {} parcels older than 48h to fail", oldParcels.size());
            
            // Emit Step 1 progress event
            if (sessionKey != null && !sessionKey.isBlank()) {
                publishSeedProgressEvent(sessionKey, SeedProgressEvent.EventType.PROGRESS, 1, 5, 
                    "Failing old parcels", 10, oldParcels.size(), 0, 0, null, null, null);
            }
            
            for (Parcel parcel : oldParcels) {
                try {
                    changeParcelStatus(parcel.getId(), ParcelEvent.CAN_NOT_DELIVERY);
                    failedOldParcelsCount++;
                } catch (Exception e) {
                    log.warn("[parcel-service] [ParcelService.autoSeedParcels] Failed to fail parcel {}: {}", parcel.getId(), e.getMessage());
                }
            }
            
            // Step 2: Get all clients from user-service
            List<UserServiceClient.UserInfo> clients = userServiceClient.getUsersByUsernamePrefix("client", 0, 1000);
            log.info("[parcel-service] [ParcelService.autoSeedParcels] Found {} clients", clients.size());
            
            // Emit Step 2 progress event
            if (sessionKey != null && !sessionKey.isBlank()) {
                publishSeedProgressEvent(sessionKey, SeedProgressEvent.EventType.PROGRESS, 2, 5, 
                    "Fetching clients", 30, failedOldParcelsCount, 0, 0, null, null, null);
            }
            
            // Step 3: Get all shops (for sender selection)
            List<UserServiceClient.UserInfo> shops = userServiceClient.getUsersByUsernamePrefix("shop", 0, 1000);
            log.info("[parcel-service] [ParcelService.autoSeedParcels] Found {} shops", shops.size());
            
            // Emit Step 3 progress event
            if (sessionKey != null && !sessionKey.isBlank()) {
                publishSeedProgressEvent(sessionKey, SeedProgressEvent.EventType.PROGRESS, 3, 5, 
                    "Fetching shops", 40, failedOldParcelsCount, 0, 0, null, null, null);
            }
            
            if (shops.isEmpty()) {
                log.warn("[parcel-service] [ParcelService.autoSeedParcels] No shops found, cannot seed parcels");
                return new AutoSeedResult(failedOldParcelsCount, 0, skippedAddressesCount, "No shops found");
            }
            
            // Step 4: Get primary addresses for shops
            Map<String, String> shopPrimaryAddressIds = new HashMap<>();
            for (UserServiceClient.UserInfo shop : shops) {
                List<UserServiceClient.UserAddressInfo> shopAddresses = userServiceClient.getUserAddressesByUserId(shop.getId());
                log.debug("[parcel-service] [ParcelService.autoSeedParcels] Shop {} has {} addresses", shop.getId(), shopAddresses.size());
                boolean foundPrimary = false;
                for (UserServiceClient.UserAddressInfo address : shopAddresses) {
                    log.debug("[parcel-service] [ParcelService.autoSeedParcels] Shop {} address {}: isPrimary={}", 
                        shop.getId(), address.getId(), address.getIsPrimary());
                    if (Boolean.TRUE.equals(address.getIsPrimary())) {
                        shopPrimaryAddressIds.put(shop.getId(), address.getId());
                        foundPrimary = true;
                        log.debug("[parcel-service] [ParcelService.autoSeedParcels] Shop {} primary address found: {}", shop.getId(), address.getId());
                        break;
                    }
                }
                if (!foundPrimary) {
                    log.warn("[parcel-service] [ParcelService.autoSeedParcels] Shop {} has no primary address", shop.getId());
                }
            }
            
            log.info("[parcel-service] [ParcelService.autoSeedParcels] Found {} shops with primary addresses out of {} shops", 
                shopPrimaryAddressIds.size(), shops.size());
            
            // Emit Step 4 progress event
            if (sessionKey != null && !sessionKey.isBlank()) {
                publishSeedProgressEvent(sessionKey, SeedProgressEvent.EventType.PROGRESS, 4, 5, 
                    "Fetching shop addresses", 50, failedOldParcelsCount, 0, 0, null, null, null);
            }
            
            if (shopPrimaryAddressIds.isEmpty()) {
                log.warn("[parcel-service] [ParcelService.autoSeedParcels] No shops with primary addresses found");
                String errorMsg = "No shops with primary addresses found";
                if (sessionKey != null && !sessionKey.isBlank()) {
                    publishSeedProgressEvent(sessionKey, SeedProgressEvent.EventType.ERROR, 4, 5, 
                        errorMsg, 50, failedOldParcelsCount, 0, 0, null, null, errorMsg);
                }
                return new AutoSeedResult(failedOldParcelsCount, 0, skippedAddressesCount, errorMsg);
            }
            
            // Step 5: For each client, check addresses and seed parcels
            java.util.Random random = new java.util.Random();
            String[] deliveryTypes = { "NORMAL", "EXPRESS", "FAST", "URGENT", "ECONOMY" };
            
            // Get all existing parcels with statuses to check
            List<Parcel> existingParcels = parcelRepository.findByStatusIn(statusesToFail);
            Set<String> addressesWithParcels = existingParcels.stream()
                .map(Parcel::getReceiverAddressId)
                .filter(addrId -> addrId != null && !addrId.isBlank())
                .collect(java.util.stream.Collectors.toSet());
            
            log.info("[parcel-service] [ParcelService.autoSeedParcels] Found {} addresses with existing parcels in target statuses", addressesWithParcels.size());
            
            int totalClients = clients.size();
            int processedClients = 0;
            
            // Emit Step 5 start event
            if (sessionKey != null && !sessionKey.isBlank()) {
                publishSeedProgressEvent(sessionKey, SeedProgressEvent.EventType.PROGRESS, 5, 5, 
                    "Processing clients", 60, failedOldParcelsCount, seededParcelsCount, skippedAddressesCount, 
                    processedClients, totalClients, null);
            }
            
            for (UserServiceClient.UserInfo client : clients) {
                try {
                    List<UserServiceClient.UserAddressInfo> clientAddresses = userServiceClient.getUserAddressesByUserId(client.getId());
                    
                    for (UserServiceClient.UserAddressInfo address : clientAddresses) {
                        // Check if address already has a parcel in target statuses
                        if (addressesWithParcels.contains(address.getId())) {
                            skippedAddressesCount++;
                            continue;
                        }
                        
                        // Select random shop
                        List<String> shopIds = new ArrayList<>(shopPrimaryAddressIds.keySet());
                        String selectedShopId = shopIds.get(random.nextInt(shopIds.size()));
                        String senderAddressId = shopPrimaryAddressIds.get(selectedShopId);
                        
                        if (senderAddressId == null) {
                            log.warn("[parcel-service] [ParcelService.autoSeedParcels] Shop {} has no primary address, skipping", selectedShopId);
                            skippedAddressesCount++;
                            continue;
                        }
                        
                        // Generate unique code
                        String code = "AUTO-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
                        
                        // Generate random delivery type and weight/value
                        String deliveryType = deliveryTypes[random.nextInt(deliveryTypes.length)];
                        double weight = 0.5 + random.nextDouble() * 9.5; // 0.5 - 10 kg
                        BigDecimal value = BigDecimal.valueOf(10000 + random.nextInt(990000)); // 10k - 1M VND
                        
                        // Create parcel
                        ParcelCreateRequest createRequest = ParcelCreateRequest.builder()
                            .code(code)
                            .senderId(selectedShopId)
                            .receiverId(client.getId())
                            .senderAddressId(senderAddressId)
                            .receiverAddressId(address.getId())
                            .deliveryType(deliveryType)
                            .weight(weight)
                            .value(value)
                            .build();
                        
                        try {
                            createParcel(createRequest);
                            seededParcelsCount++;
                            addressesWithParcels.add(address.getId()); // Mark as having parcel
                            log.debug("[parcel-service] [ParcelService.autoSeedParcels] Created parcel {} for client {} address {}", 
                                code, client.getId(), address.getId());
                        } catch (Exception e) {
                            log.warn("[parcel-service] [ParcelService.autoSeedParcels] Failed to create parcel for client {} address {}: {}", 
                                client.getId(), address.getId(), e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.warn("[parcel-service] [ParcelService.autoSeedParcels] Failed to process client {}: {}", client.getId(), e.getMessage());
                }
                
                // Emit progress event every 10 clients or at the end
                processedClients++;
                if (sessionKey != null && !sessionKey.isBlank() && (processedClients % 10 == 0 || processedClients == totalClients)) {
                    int progress = 60 + (int) ((processedClients * 40.0) / totalClients);
                    publishSeedProgressEvent(sessionKey, SeedProgressEvent.EventType.PROGRESS, 5, 5, 
                        "Processing clients", progress, failedOldParcelsCount, seededParcelsCount, skippedAddressesCount, 
                        processedClients, totalClients, null);
                }
            }
            
            log.info("[parcel-service] [ParcelService.autoSeedParcels] Auto seed completed: {} old parcels failed, {} new parcels seeded, {} addresses skipped", 
                failedOldParcelsCount, seededParcelsCount, skippedAddressesCount);
            
            // Emit COMPLETED event
            if (sessionKey != null && !sessionKey.isBlank()) {
                publishSeedProgressEvent(sessionKey, SeedProgressEvent.EventType.COMPLETED, 5, 5, 
                    "Completed", 100, failedOldParcelsCount, seededParcelsCount, skippedAddressesCount, 
                    processedClients, totalClients, null);
            }
            
            return new AutoSeedResult(failedOldParcelsCount, seededParcelsCount, skippedAddressesCount, null);
            
        } catch (Exception e) {
            log.error("[parcel-service] [ParcelService.autoSeedParcels] Error during auto seed", e);
            String errorMsg = "Error: " + e.getMessage();
            
            // Emit ERROR event
            if (sessionKey != null && !sessionKey.isBlank()) {
                publishSeedProgressEvent(sessionKey, SeedProgressEvent.EventType.ERROR, null, 5, 
                    "Error occurred", null, failedOldParcelsCount, seededParcelsCount, skippedAddressesCount, 
                    null, null, errorMsg);
            }
            
            return new AutoSeedResult(failedOldParcelsCount, seededParcelsCount, skippedAddressesCount, errorMsg);
        }
    }
    
    /**
     * Publish seed progress event to Kafka
     */
    private void publishSeedProgressEvent(String sessionKey, SeedProgressEvent.EventType eventType, 
            Integer currentStep, Integer totalSteps, String stepDescription, Integer progress,
            Integer failedOldParcelsCount, Integer seededParcelsCount, Integer skippedAddressesCount,
            Integer currentClient, Integer totalClients, String errorMessage) {
        try {
            SeedProgressEvent event = SeedProgressEvent.builder()
                .sessionKey(sessionKey)
                .eventType(eventType)
                .currentStep(currentStep)
                .totalSteps(totalSteps)
                .stepDescription(stepDescription)
                .progress(progress)
                .failedOldParcelsCount(failedOldParcelsCount)
                .seededParcelsCount(seededParcelsCount)
                .skippedAddressesCount(skippedAddressesCount)
                .currentClient(currentClient)
                .totalClients(totalClients)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
            
            kafkaTemplate.send(KafkaConfig.TOPIC_SEED_PROGRESS, sessionKey, event);
            
            log.debug("[parcel-service] [ParcelService.publishSeedProgressEvent] Published seed progress event: sessionKey={}, eventType={}, progress={}%", 
                sessionKey, eventType, progress);
        } catch (Exception e) {
            log.error("[parcel-service] [ParcelService.publishSeedProgressEvent] Failed to publish seed progress event", e);
            // Don't throw - progress event failure shouldn't break the seed process
        }
    }
    
    /**
     * Result holder for auto seed operation
     */
    public static class AutoSeedResult {
        public final int failedOldParcelsCount;
        public final int seededParcelsCount;
        public final int skippedAddressesCount;
        public final String errorMessage;
        
        public AutoSeedResult(int failedOldParcelsCount, int seededParcelsCount, int skippedAddressesCount, String errorMessage) {
            this.failedOldParcelsCount = failedOldParcelsCount;
            this.seededParcelsCount = seededParcelsCount;
            this.skippedAddressesCount = skippedAddressesCount;
            this.errorMessage = errorMessage;
        }
    }
}
