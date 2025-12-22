package com.ds.session.session_service.application.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ds.session.session_service.common.entities.dto.response.DeliveryProofResponse;
import com.ds.session.session_service.common.interfaces.IDeliveryProofService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/api/v1/delivery-proofs")
@RequiredArgsConstructor
public class DeliveryProofController {

    private final IDeliveryProofService proofQueryService;

    @GetMapping("/assignments/{id}")
    public List<DeliveryProofResponse> getByAssignment(
        @PathVariable UUID id
    ) {
        return proofQueryService.getProofsByAssignment(id);    
    }

    @GetMapping("/parcels/{parcelId}")
    public List<DeliveryProofResponse> getByParcel(
        @PathVariable String parcelId
    ) {
        return proofQueryService.getProofsByParcel(parcelId);
    }

}
