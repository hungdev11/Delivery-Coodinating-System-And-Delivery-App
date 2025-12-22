package com.ds.session.session_service.business.v1.services;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ds.session.session_service.app_context.models.DeliveryAssignment;
import com.ds.session.session_service.app_context.repositories.DeliveryAssignmentRepository;
import com.ds.session.session_service.app_context.repositories.DeliveryProofRepository;
import com.ds.session.session_service.common.entities.dto.response.DeliveryProofResponse;
import com.ds.session.session_service.common.exceptions.ResourceNotFound;
import com.ds.session.session_service.common.interfaces.IDeliveryProofService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeliveryProofService implements IDeliveryProofService {

    private final DeliveryProofRepository proofRepo;
    private final DeliveryAssignmentRepository assignmentRepo;

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryProofResponse> getProofsByAssignment(
            UUID assignmentId
    ) {
        if (!assignmentRepo.existsById(assignmentId)) {
            throw new ResourceNotFound("Assignment not found");
        }

        return proofRepo.findByAssignmentId(assignmentId)
            .stream()
            .map(DeliveryProofResponse::from)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryProofResponse> getProofsByParcel(
            String parcelId
    ) {
        List<DeliveryAssignment> assignments =
            assignmentRepo.findAllByParcelId(parcelId);

        if (assignments.isEmpty()) {
            throw new ResourceNotFound("Parcel not found");
        }

        return assignments.stream()
            .flatMap(a ->
                proofRepo.findByAssignmentId(a.getId()).stream()
            )
            .map(DeliveryProofResponse::from)
            .toList();
    }

}
