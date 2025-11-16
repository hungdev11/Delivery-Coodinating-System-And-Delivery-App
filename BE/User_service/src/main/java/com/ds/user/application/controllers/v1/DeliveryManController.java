package com.ds.user.application.controllers.v1;

import com.ds.user.common.entities.common.BaseResponse;
import com.ds.user.common.entities.dto.deliveryman.DeliveryManDto;
import com.ds.user.common.interfaces.IDeliveryManService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * REST API Controller for Delivery Man Management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Delivery Man", description = "Delivery Man Management API")
public class DeliveryManController {

    private final IDeliveryManService deliveryManService;

    @GetMapping("/{userId}/delivery-man")
    @Operation(summary = "Get delivery man by user ID")
    public ResponseEntity<BaseResponse<DeliveryManDto>> getDeliveryManByUserId(@PathVariable String userId) {
        log.info("GET /api/v1/users/{}/delivery-man - Get delivery man by user ID", userId);
        
        Optional<DeliveryManDto> deliveryManOpt = deliveryManService.getDeliveryManByUserId(userId);
        if (deliveryManOpt.isPresent()) {
            return ResponseEntity.ok(BaseResponse.success(deliveryManOpt.get()));
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.error("Delivery man not found for user ID: " + userId));
        }
    }
}
