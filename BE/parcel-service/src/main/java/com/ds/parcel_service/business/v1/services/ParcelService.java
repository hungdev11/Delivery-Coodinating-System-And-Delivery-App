package com.ds.parcel_service.business.v1.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ds.parcel_service.app_context.models.Parcel;
import com.ds.parcel_service.app_context.models.ParcelDestination;
import com.ds.parcel_service.app_context.repositories.ParcelDestinationRepository;
import com.ds.parcel_service.app_context.repositories.ParcelRepository;
import com.ds.parcel_service.application.client.DesDetail;
import com.ds.parcel_service.application.client.DestinationResponse;
import com.ds.parcel_service.application.client.ListAddressResponse;
import com.ds.parcel_service.application.client.ZoneClient;
import com.ds.parcel_service.common.entities.dto.request.ParcelCreateRequest;
import com.ds.parcel_service.common.entities.dto.request.ParcelFilterRequest;
import com.ds.parcel_service.common.entities.dto.request.ParcelUpdateRequest;
import com.ds.parcel_service.common.entities.dto.response.PageResponse;
import com.ds.parcel_service.common.entities.dto.response.ParcelResponse;
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
            log.error("Missing state handler for status: {}", currentStatus);
            throw new IllegalStateException("Missing state handler for current status.");
        }
        
        ParcelStatus nextStatus = currentStateObject.handleTransition(event);

        if (currentStatus.equals(nextStatus)) {
            log.info("Parcel {} state remains {}. Event processed: {}", parcelId, currentStatus, event);
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
        return parcelRepository.save(parcel);
    }

    @Override
    public ParcelResponse changeParcelStatus(UUID parcelId, ParcelEvent event) {
        return toDto(processTransition(parcelId, event));
    }

    @Override
    @Transactional
    public ParcelResponse createParcel(ParcelCreateRequest request) {
        validateUniqueCode(request.getCode());
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

        // [Logic: Call zone service, create destination, get receiver info...]
        // find exiting destination from address text, if not found create new one
        // map relationship between parcel and destination
        // call to user service to get receiver/sender info

        // DestinationResponse des = zoneClient.createDestination(
        //     new CreateDestinationRequest(
        //         parcel.getSendTo(), parcel.getSendTo(), request.getLat(), request.getLon()
        //         )
        //     );

        ListAddressResponse resp = zoneClient.getNearestDestination(request.getLat(), request.getLon());

        DesDetail des = !resp.getResult().isEmpty() ? resp.getResult().get(0) : null;
        
        Parcel savedParcel = parcelRepository.save(parcel);

        ParcelDestination pd = ParcelDestination.builder()
            .destinationId(des.getId())
            .destinationType(DestinationType.PRIMARY)
            .isCurrent(true)
            .isOriginal(true)
            .parcel(savedParcel)
            .build();

        parcelDestinationRepository.save(pd);
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
    public PageResponse<ParcelResponse> getParcelsV2(com.ds.parcel_service.common.entities.dto.request.PagingRequestV2 request) {
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

    private ParcelResponse toDto(Parcel parcel) {
        // [Logic: get phone number here]
        return ParcelResponse.builder()
                            .id(parcel.getId().toString())
                            .code(parcel.getCode())
                            .deliveryType(parcel.getDeliveryType())
                            .senderId(parcel.getSenderId())
                            .receiverId(parcel.getReceiverId())
                            .receiveFrom(parcel.getReceiveFrom())
                            .targetDestination(parcel.getSendTo())
                            .weight(parcel.getWeight())
                            .value(parcel.getValue())
                            .status(parcel.getStatus())
                            .createdAt(parcel.getCreatedAt())
                            .updatedAt(parcel.getUpdatedAt())
                            .deliveredAt(parcel.getDeliveredAt())
                            .windowStart(parcel.getWindowStart())
                            .windowEnd(parcel.getWindowEnd())
                            .build();
    }

    private ParcelResponse toDtoWithLocation(Parcel parcel, DestinationResponse<DesDetail> des) {
        ParcelResponse response = toDto(parcel);
        log.info("before {} {}", des.getResult().getLat(), des.getResult().getLon());
        response.setLat(des.getResult().getLat());
        response.setLon(des.getResult().getLon());
        log.info("after {} {}", response.getLat(), response.getLon());
        return response;
    }

    private void validateUniqueCode(String code) {
        if (parcelRepository.existsByCode(code)) {
            throw new IllegalStateException("Parcel with code already exists");
        }
    }

    private Parcel getParcel(UUID id) {
        return parcelRepository.findById(id).orElseThrow(()->{
            return new ResourceNotFound("Parcel not found");
        });
    }

    @Override
    public Map<String, ParcelResponse> fetchParcelsBulk(List<UUID> parcelIds) {
        return parcelIds.stream()
            .collect(Collectors.toMap(
                UUID::toString,
                parcelId -> {
                    try {
                        return getParcelById(parcelId);
                    } catch (Exception e) {
                        log.error("Failed to fetch parcel info for {}: {}", parcelId, e.getMessage());
                        return null; // Bỏ qua parcel lỗi
                    }
                }
            ));
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

    //update address
    // parcel staticstic
}
