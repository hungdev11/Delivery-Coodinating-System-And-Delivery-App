package com.ds.user.application.controllers.v2;

import com.ds.user.common.entities.common.BaseResponse;
import com.ds.user.common.entities.common.PagingRequestV2;
import com.ds.user.common.entities.common.paging.PagedData;
import com.ds.user.common.entities.dto.deliveryman.DeliveryManDto;
import com.ds.user.common.interfaces.IDeliveryManService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * V2 API Controller for Delivery Man (Shipper) management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v2/users/shippers")
@RequiredArgsConstructor
@Tag(name = "Delivery Man V2", description = "Delivery man (shipper) management API V2")
public class DeliveryManControllerV2 {

    private final IDeliveryManService deliveryManService;

    @PostMapping
    @Operation(summary = "Get delivery men with enhanced filtering (V2)")
    public ResponseEntity<BaseResponse<PagedData<DeliveryManDto>>> getDeliveryMans(
            @Valid @RequestBody PagingRequestV2 query) {
        log.info("POST /api/v2/users/shippers - Get delivery men with enhanced filtering (V2)");
        PagedData<DeliveryManDto> pagedData = deliveryManService.getDeliveryMansV2(query);
        return ResponseEntity.ok(BaseResponse.success(pagedData));
    }
}
