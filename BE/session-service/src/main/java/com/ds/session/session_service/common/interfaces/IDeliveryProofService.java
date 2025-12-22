package com.ds.session.session_service.common.interfaces;

import java.util.List;
import java.util.UUID;

import com.ds.session.session_service.common.entities.dto.response.DeliveryProofResponse;

public interface IDeliveryProofService {
    List<DeliveryProofResponse> getProofsByAssignment(UUID assignmentId);
    List<DeliveryProofResponse> getProofsByParcel(String parcelId);
}
