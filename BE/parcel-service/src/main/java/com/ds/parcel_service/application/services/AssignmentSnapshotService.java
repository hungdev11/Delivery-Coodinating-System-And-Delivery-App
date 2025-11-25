package com.ds.parcel_service.application.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ds.parcel_service.app_context.models.AssignmentSnapshot;
import com.ds.parcel_service.app_context.repositories.AssignmentSnapshotRepository;
import com.ds.parcel_service.application.client.SessionServiceClient;
import com.ds.parcel_service.application.client.SessionServiceClient.LatestAssignmentInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentSnapshotService {

    private final AssignmentSnapshotRepository assignmentSnapshotRepository;
    private final SessionServiceClient sessionServiceClient;

    @Transactional
    public AssignmentSnapshot getOrFetch(UUID parcelId) {
        return assignmentSnapshotRepository.findFirstByParcelIdOrderByUpdatedAtDesc(parcelId)
            .orElseGet(() -> fetchAndPersist(parcelId));
    }

    @Transactional
    public AssignmentSnapshot refreshFromRemote(UUID parcelId) {
        return fetchAndPersist(parcelId);
    }

    private AssignmentSnapshot fetchAndPersist(UUID parcelId) {
        LatestAssignmentInfo info = sessionServiceClient.getLatestAssignmentForParcel(parcelId);
        AssignmentSnapshot snapshot = AssignmentSnapshot.builder()
            .assignmentId(info.getAssignmentId())
            .parcelId(parcelId)
            .sessionId(info.getSessionId())
            .deliveryManId(info.getDeliveryManId())
            .status(info.getStatus())
            .updatedAt(LocalDateTime.now())
            .build();
        log.debug("[parcel-service] [AssignmentSnapshotService.fetchAndPersist] Persisting assignment snapshot for parcel {} -> assignment {}", parcelId, info.getAssignmentId());
        return assignmentSnapshotRepository.save(snapshot);
    }

    @Transactional
    public void updateStatus(UUID assignmentId, String newStatus) {
        assignmentSnapshotRepository.findById(assignmentId).ifPresent(snapshot -> {
            snapshot.setStatus(newStatus);
            snapshot.setUpdatedAt(LocalDateTime.now());
            assignmentSnapshotRepository.save(snapshot);
        });
    }
}
