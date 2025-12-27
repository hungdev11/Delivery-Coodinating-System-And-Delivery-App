package com.ds.user.business.v1.services;

import com.ds.user.app_context.repositories.DeliveryManRepository;
import com.ds.user.app_context.repositories.UserRepository;
import com.ds.user.common.entities.base.DeliveryMan;
import com.ds.user.common.entities.base.User;
import com.ds.user.common.entities.base.WorkingZone;
import com.ds.user.common.entities.common.PagingRequest;
import com.ds.user.common.entities.common.PagingRequestV2;
import com.ds.user.common.entities.common.paging.PagedData;
import com.ds.user.common.entities.common.paging.Paging;
import com.ds.user.common.entities.dto.deliveryman.CreateDeliveryManRequest;
import com.ds.user.common.entities.dto.deliveryman.DeliveryManDto;
import com.ds.user.common.entities.dto.deliveryman.DeliveryManSessionInfo;
import com.ds.user.common.entities.dto.deliveryman.UpdateDeliveryManRequest;
import com.ds.user.common.helper.FilterableFieldRegistry;
import com.ds.user.common.helper.GenericQueryService;
import com.ds.user.common.interfaces.IDeliveryManService;
import com.ds.user.common.utils.EnhancedQueryParser;
import com.ds.user.common.utils.EnhancedQueryParserV2;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeliveryManService implements IDeliveryManService {

    @Autowired
    private DeliveryManRepository deliveryManRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FilterableFieldRegistry fieldRegistry;

    @Autowired
    private com.ds.user.app_context.repositories.WorkingZoneRepository workingZoneRepository;
    
    private final WebClient sessionServiceWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${services.session.base-url}")
    private String sessionServiceBaseUrl;
    
    public DeliveryManService(
            @Autowired(required = false) @Qualifier("sessionServiceWebClient") WebClient sessionServiceWebClient) {
        this.sessionServiceWebClient = sessionServiceWebClient;
    }

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
        
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void deleteDeliveryMan(UUID id) {
        DeliveryMan deliveryMan = deliveryManRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery man not found with id: " + id));
        
        String userId = deliveryMan.getUser().getId();
        
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

        // Map to DTOs
        List<DeliveryManDto> dtos = page.getContent().stream().map(this::mapToDto).collect(Collectors.toList());
        
        // Enrich with session information if WebClient is available
        if (sessionServiceWebClient != null) {
            enrichWithSessionInfo(dtos);
        }

        return PagedData.<DeliveryManDto>builder()
                .data(dtos)
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
    
    /**
     * Enrich delivery man DTOs with session information from session-service
     */
    private void enrichWithSessionInfo(List<DeliveryManDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        
        // Fetch session info for all delivery men in parallel
        Map<String, DeliveryManSessionInfo> sessionInfoMap = dtos.stream()
                .collect(Collectors.toMap(
                        DeliveryManDto::getUserId,
                        dto -> {
                            try {
                                return fetchSessionInfo(dto.getUserId());
                            } catch (Exception e) {
                                log.warn("Failed to fetch session info for delivery man {}: {}", dto.getUserId(), e.getMessage());
                                return DeliveryManSessionInfo.builder()
                                        .hasActiveSession(false)
                                        .lastSessionStartTime(null)
                                        .build();
                            }
                        }
                ));
        
        // Enrich DTOs with session info
        dtos.forEach(dto -> {
            DeliveryManSessionInfo sessionInfo = sessionInfoMap.get(dto.getUserId());
            if (sessionInfo != null) {
                dto.setHasActiveSession(sessionInfo.getHasActiveSession());
                dto.setLastSessionStartTime(sessionInfo.getLastSessionStartTime());
            }
        });
    }
    
    /**
     * Fetch session information for a delivery man from session-service
     */
    private DeliveryManSessionInfo fetchSessionInfo(String userId) {
        try {
            String responseBody = sessionServiceWebClient.get()
                    .uri("/api/v1/sessions/drivers/{deliveryManId}/session-info", userId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (responseBody == null || responseBody.isBlank()) {
                log.debug("Empty response from session-service for delivery man {}", userId);
                return DeliveryManSessionInfo.builder()
                        .hasActiveSession(false)
                        .lastSessionStartTime(null)
                        .build();
            }
            
            // Parse BaseResponse wrapper
            com.fasterxml.jackson.databind.JsonNode jsonResponse = objectMapper.readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode resultNode = jsonResponse.get("result");
            if (resultNode == null) {
                log.warn("No 'result' field in session-service response for delivery man {}", userId);
                return DeliveryManSessionInfo.builder()
                        .hasActiveSession(false)
                        .lastSessionStartTime(null)
                        .build();
            }
            
            // Parse session info
            DeliveryManSessionInfo sessionInfo = objectMapper.treeToValue(resultNode, DeliveryManSessionInfo.class);
            return sessionInfo != null ? sessionInfo : DeliveryManSessionInfo.builder()
                    .hasActiveSession(false)
                    .lastSessionStartTime(null)
                    .build();
                    
        } catch (WebClientResponseException.NotFound e) {
            // Delivery man has no sessions - this is normal
            log.debug("No session info found for delivery man {} (404)", userId);
            return DeliveryManSessionInfo.builder()
                    .hasActiveSession(false)
                    .lastSessionStartTime(null)
                    .build();
        } catch (Exception e) {
            log.error("Error fetching session info for delivery man {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch session info for delivery man " + userId, e);
        }
    }

    private DeliveryManDto mapToDto(DeliveryMan deliveryMan) {
        User user = deliveryMan.getUser();
        
        // Get primary zoneId from working zones (zone with order = 1, or first zone if no order=1)
        String primaryZoneId = null;
        try {
            List<WorkingZone> workingZones = 
                workingZoneRepository.findByDeliveryManIdOrderByOrderAsc(deliveryMan.getId());
            if (!workingZones.isEmpty()) {
                // Get zone with order = 1 (highest priority), or first zone if no order=1 exists
                primaryZoneId = workingZones.stream()
                    .filter(wz -> wz.getOrder() != null && wz.getOrder() == 1)
                    .findFirst()
                    .map(WorkingZone::getZoneId)
                    .orElse(workingZones.get(0).getZoneId());
            }
        } catch (Exception e) {
            log.debug("Error fetching working zones for delivery man {}: {}", deliveryMan.getId(), e.getMessage());
            // Continue without zoneId if error occurs
        }
        
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
                // Session information (will be enriched later if WebClient is available)
                .hasActiveSession(null)
                .lastSessionStartTime(null)
                // Zone information
                .zoneId(primaryZoneId)
                .build();
    }
}
