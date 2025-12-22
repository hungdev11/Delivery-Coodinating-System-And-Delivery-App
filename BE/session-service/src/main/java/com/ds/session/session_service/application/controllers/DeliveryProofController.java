package com.ds.session.session_service.application.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds.session.session_service.common.entities.dto.common.BaseResponse;
import com.ds.session.session_service.common.entities.dto.response.DeliveryProofResponse;
import com.ds.session.session_service.common.interfaces.IDeliveryProofService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/delivery-proofs")
@RequiredArgsConstructor
public class DeliveryProofController {

    private final IDeliveryProofService proofQueryService;

    /**
     * Lấy danh sách bằng chứng theo assignmentId
     */
    @GetMapping("/assignments/{id}")
    public ResponseEntity<BaseResponse<List<DeliveryProofResponse>>> getByAssignment(
        @PathVariable UUID id
    ) {
        List<DeliveryProofResponse> proofs = proofQueryService.getProofsByAssignment(id);
        return ResponseEntity.ok(BaseResponse.success(proofs));    
    }

    /**
     * Lấy danh sách bằng chứng theo parcelId
     * Trả về tất cả proofs từ tất cả assignments của parcel đó
     */
    @GetMapping("/parcels/{parcelId}")
    public ResponseEntity<BaseResponse<List<DeliveryProofResponse>>> getByParcel(
        @PathVariable String parcelId
    ) {
        List<DeliveryProofResponse> proofs = proofQueryService.getProofsByParcel(parcelId);
        return ResponseEntity.ok(BaseResponse.success(proofs));
    }

}
