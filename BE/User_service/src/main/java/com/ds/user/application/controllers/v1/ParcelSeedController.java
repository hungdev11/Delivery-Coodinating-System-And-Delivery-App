package com.ds.user.application.controllers.v1;

import com.ds.user.application.startup.data.services.ParcelSeedService;
import com.ds.user.common.entities.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.ds.user.application.startup.data.services.ParcelSeedService.SeedParcelsResult;

/**
 * REST API Controller for Parcel Seeding
 * Allows manual seeding of parcels from the web UI
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parcels/seed")
@RequiredArgsConstructor
@Tag(name = "Parcel Seed", description = "Parcel Seeding API")
public class ParcelSeedController {

    private final ParcelSeedService parcelSeedService;

    @Data
    public static class SeedParcelsRequest {
        /**
         * Number of parcels to create (random shop/client selection)
         * If not provided, uses default from config
         */
        private Integer count;

        /**
         * Optional: Specific shop ID to use as sender
         * If not provided, randomly selects from available shops
         */
        private String shopId;

        /**
         * Optional: Specific client ID to use as receiver
         * If not provided, randomly selects from available clients
         */
        private String clientId;
    }

    @Data
    public static class SeedParcelsResponse {
        private int successCount;
        private int failCount;
        private int total;
        private String message;
    }

    /**
     * POST /api/v1/parcels/seed
     * Seed parcels randomly or with specific shop/client
     */
    @PostMapping
    @Operation(summary = "Seed parcels", description = "Create parcels randomly or with specific shop/client. Uses primary addresses automatically.")
    public ResponseEntity<BaseResponse<SeedParcelsResponse>> seedParcels(
            @Valid @RequestBody(required = false) SeedParcelsRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        log.info("POST /api/v1/parcels/seed - Seed parcels");
        log.debug("Request: count={}, shopId={}, clientId={}", 
                request != null ? request.getCount() : null,
                request != null ? request.getShopId() : null,
                request != null ? request.getClientId() : null);

        try {
            SeedParcelsResult result;
            if (request != null && (request.getShopId() != null || request.getClientId() != null)) {
                // Seed with specific shop/client
                result = parcelSeedService.seedParcelsWithSelection(
                        request.getCount() != null ? request.getCount() : 1,
                        request.getShopId(),
                        request.getClientId(),
                        authorization);
            } else {
                // Seed randomly
                int count = request != null && request.getCount() != null ? request.getCount() : 20;
                result = parcelSeedService.seedParcels(count, authorization);
            }

            SeedParcelsResponse response = new SeedParcelsResponse();
            response.setSuccessCount(result.successCount);
            response.setFailCount(result.failCount);
            response.setTotal(result.total);
            response.setMessage(String.format("Successfully created %d parcel(s), %d failed", 
                    result.successCount, result.failCount));

            return ResponseEntity.ok(BaseResponse.success(response));
        } catch (Exception e) {
            log.error("Error seeding parcels: {}", e.getMessage(), e);
            SeedParcelsResponse errorResponse = new SeedParcelsResponse();
            errorResponse.setSuccessCount(0);
            errorResponse.setFailCount(0);
            errorResponse.setTotal(0);
            errorResponse.setMessage("Error seeding parcels: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(BaseResponse.<SeedParcelsResponse>builder()
                            .result(errorResponse)
                            .message("Error seeding parcels: " + e.getMessage())
                            .build());
        }
    }
}
