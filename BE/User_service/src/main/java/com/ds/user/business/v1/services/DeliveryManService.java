package com.ds.user.business.v1.services;

import com.ds.user.app_context.repositories.DeliveryManRepository;
import com.ds.user.app_context.repositories.UserRepository;
import com.ds.user.common.entities.base.DeliveryMan;
import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.common.PagingRequest;
import com.ds.user.common.entities.common.PagingRequestV2;
import com.ds.user.common.entities.common.paging.PagedData;
import com.ds.user.common.entities.common.paging.Paging;
import com.ds.user.common.entities.dto.deliveryman.CreateDeliveryManRequest;
import com.ds.user.common.entities.dto.deliveryman.DeliveryManDto;
import com.ds.user.common.entities.dto.deliveryman.UpdateDeliveryManRequest;
import com.ds.user.common.helper.FilterableFieldRegistry;
import com.ds.user.common.helper.GenericQueryService;
import com.ds.user.common.interfaces.IDeliveryManService;
import com.ds.user.common.utils.EnhancedQueryParser;
import com.ds.user.common.utils.EnhancedQueryParserV2;
import com.ds.user.infrastructure.kafka.UserEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class DeliveryManService implements IDeliveryManService {

    @Autowired
    private DeliveryManRepository deliveryManRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FilterableFieldRegistry fieldRegistry;

    @Autowired(required = false)
    private UserEventPublisher userEventPublisher;

    @Override
    @Transactional
    public DeliveryManDto createDeliveryMan(CreateDeliveryManRequest request) {
        // Check if user exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        // Check if delivery man already exists for this user
        if (deliveryManRepository.existsByUserId(request.getUserId())) {
            throw new RuntimeException("Delivery man already exists for user: " + request.getUserId());
        }

        DeliveryMan deliveryMan = DeliveryMan.builder()
                .user(user)
                .vehicleType(request.getVehicleType())
                .capacityKg(request.getCapacityKg())
                .build();

        DeliveryMan saved = deliveryManRepository.save(deliveryMan);
        
        // Publish event for snapshot synchronization
        if (userEventPublisher != null) {
            userEventPublisher.publishDeliveryManCreated(saved, user);
        }
        
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public DeliveryManDto updateDeliveryMan(UUID id, UpdateDeliveryManRequest request) {
        DeliveryMan deliveryMan = deliveryManRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery man not found with id: " + id));

        deliveryMan.setVehicleType(request.getVehicleType());
        if (request.getCapacityKg() != null) {
            deliveryMan.setCapacityKg(request.getCapacityKg());
        }

        DeliveryMan updated = deliveryManRepository.save(deliveryMan);
        
        // Get user for event publishing
        User user = updated.getUser();
        
        // Publish event for snapshot synchronization
        if (userEventPublisher != null) {
            userEventPublisher.publishDeliveryManUpdated(updated, user);
        }
        
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void deleteDeliveryMan(UUID id) {
        DeliveryMan deliveryMan = deliveryManRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery man not found with id: " + id));
        
        String userId = deliveryMan.getUser().getId();
        
        // Publish event before deletion
        if (userEventPublisher != null) {
            userEventPublisher.publishDeliveryManDeleted(userId);
        }
        
        deliveryManRepository.deleteById(id);
    }

    @Override
    public Optional<DeliveryManDto> getDeliveryMan(UUID id) {
        return deliveryManRepository.findById(id)
                .map(this::mapToDto);
    }

    @Override
    public Optional<DeliveryManDto> getDeliveryManByUserId(String userId) { // Changed from UUID to String
        return deliveryManRepository.findByUserId(userId)
                .map(this::mapToDto);
    }

    @Override
    public PagedData<DeliveryManDto> getDeliveryMans(PagingRequest query) {
        // Initialize field registry for DeliveryMan entity if not already done
        if (fieldRegistry.getFilterableFields(DeliveryMan.class).isEmpty()) {
            fieldRegistry.autoDiscoverFields(DeliveryMan.class);
        }

        // Set the field registry for enhanced query parser
        EnhancedQueryParser.setFieldRegistry(fieldRegistry);

        // Use GenericQueryService for consistent query handling
        PagedData<DeliveryMan> pagedData = GenericQueryService.executeQuery(deliveryManRepository, query, DeliveryMan.class);

        // Map entities to DTOs
        return PagedData.<DeliveryManDto>builder()
                .data(pagedData.getData().stream().map(this::mapToDto).toList())
                .page(pagedData.getPage())
                .build();
    }

    @Override
    public PagedData<DeliveryManDto> getDeliveryMansV2(PagingRequestV2 query) {
        if (fieldRegistry.getFilterableFields(DeliveryMan.class).isEmpty()) {
            fieldRegistry.autoDiscoverFields(DeliveryMan.class);
        }

        Specification<DeliveryMan> spec = Specification.where(null);
        if (query.getFiltersOrNull() != null) {
            spec = EnhancedQueryParserV2.parseFilterGroup(query.getFiltersOrNull(), DeliveryMan.class);
        }

        Sort sort = query.getSortsOrEmpty().isEmpty()
                ? Sort.by(Sort.Direction.DESC, "id")
                : EnhancedQueryParser.parseSortConfigs(query.getSortsOrEmpty(), DeliveryMan.class);

        Pageable pageable = PageRequest.of(query.getPage(), query.getSize(), sort);

        // findAll with EntityGraph will eagerly load user relationship
        Page<DeliveryMan> page = deliveryManRepository.findAll(spec, pageable);

        return PagedData.<DeliveryManDto>builder()
                .data(page.getContent().stream().map(this::mapToDto).toList())
                .page(new Paging<>(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        null,
                        query.getSortsOrEmpty(),
                        query.getSelectedOrEmpty()
                ))
                .build();
    }

    private DeliveryManDto mapToDto(DeliveryMan deliveryMan) {
        User user = deliveryMan.getUser();
        return DeliveryManDto.builder()
                .id(deliveryMan.getId())
                .userId(user.getId())
                .vehicleType(deliveryMan.getVehicleType())
                .capacityKg(deliveryMan.getCapacityKg())
                .createdAt(deliveryMan.getCreatedAt())
                .updatedAt(deliveryMan.getUpdatedAt())
                // User information
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .status(user.getStatus() != null ? user.getStatus().name() : null)
                .build();
    }
}
